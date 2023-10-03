/**
 * This file is part of the JELY distribution (https://github.com/mad-lab-fau/JELY).
 * Copyright (c) 2015-2020 Machine Learning and Data Analytics Lab, Friedrich-Alexander-Universität Erlangen-Nürnberg (FAU).
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 * <p>
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package jely.annotations;

import jely.Ecg;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Information about a subject for which an ECG was recorded.
 *
 * @author Stefan Gradl
 */
public class SubjectInfo {
    private Ecg ecg;
    private SubjectSex sex;
    private Date birthdate = null;

    /**
     * Age of the subject at the time of the ECG measurement.
     */
    private int age;

    /**
     * Weight (body mass) in kg.
     */
    private double weight;

    /**
     * Body height in meters.
     */
    private double height;

    private String notes;

    /**
     * Construct an unknown subject info.
     */
    public SubjectInfo() {
        ecg = null;
        sex = SubjectSex.UNKNOWN;
        birthdate = null;
        age = 0;
        weight = 0;
        height = 0;
        notes = "";
    }

    /**
     * Construct an unknown subject info which relates to an ECG.
     */
    public SubjectInfo(Ecg ecg) {
        this();
        this.ecg = ecg;
    }

    public SubjectInfo(SubjectSex sex, Date birthdate, double weight, double height, String notes) {
        this.sex = sex;
        this.birthdate = birthdate;
        this.weight = weight;
        this.height = height;
        this.notes = notes;
    }

    /**
     * Constructs a subject using a date string and age value. The constructor figures out which of the two fields will
     * be used (birthdate is priorized, assuming that the age is calculated based on the current datetime).
     *
     * @param sex
     * @param birthdate
     * @param age
     * @param weight
     * @param height
     * @param notes
     */
    public SubjectInfo(SubjectSex sex, String birthdateAsString, int age, double weight, double height, String notes) {
        this.sex = sex;
        this.weight = weight;
        this.height = height;
        this.notes = notes;

        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        try {
            Date birthdate = dateFormat.parse(birthdateAsString);
            Date now = new Date();
            if (SubjectInfo.calculateAge(now, birthdate) > 200)
                setBirthdate(null);
            else
                setBirthdate(birthdate);
        } catch (ParseException e) {
            setAge(age);
        }
    }

    public SubjectInfo(Ecg ecg, SubjectSex sex, Date birthdate, double weight, double height, String notes) {
        this(sex, birthdate, weight, height, notes);
        this.ecg = ecg;
        age = calculateAge(ecg.getDate(), birthdate);
    }

    public SubjectInfo(Ecg ecg, SubjectSex sex, int age, double weight, double height, String notes) {
        this.ecg = ecg;
        this.sex = sex;
        this.birthdate = null;
        this.weight = weight;
        this.height = height;
        this.notes = notes;
        this.age = age;
    }

    public static int calculateAge(Date atDate, Date birthdate) {
        if (birthdate == null || atDate == null)
            return 0;

        // convert time span from ms to days
        long ageSpan = (atDate.getTime() - birthdate.getTime()) / 1000 / 60 / 60 / 24;
        return (int) (ageSpan / 365.25);
    }

    /**
     * @return the sex
     */
    public SubjectSex getSex() {
        return sex;
    }

    /**
     * @return the ecg
     */
    public Ecg getEcg() {
        return ecg;
    }

    /**
     * @param ecg the ecg to set
     */
    public void setEcg(Ecg ecg) {
        this.ecg = ecg;
    }

    /**
     * @return the birthdate, which can be <code>null</code> if it is not known or anonymized.
     */
    public Date getBirthdate() {
        return birthdate;
    }

    /**
     * @return the age of the subject at the time of the ECG measurement.
     */
    public int getAge() {
        if (birthdate == null || age > 0 || ecg == null)
            return age;
        return calculateAge(ecg.getDate(), birthdate);
    }

    /**
     * @param age the age to set
     */
    public void setAge(int age) {
        this.age = age;
    }

    /**
     * @return the weight in kg.
     */
    public double getWeight() {
        return weight;
    }

    /**
     * @return the height in m.
     */
    public double getHeight() {
        return height;
    }

    /**
     * @return the notes
     */
    public String getNotes() {
        return notes;
    }

    /**
     * @param notes the notes to set
     */
    public void setNotes(String notes) {
        this.notes = notes;
    }

    /**
     * @param sex the sex to set
     */
    public void setSex(SubjectSex sex) {
        this.sex = sex;
    }

    /**
     * @param birthdate the birthdate to set
     */
    public void setBirthdate(Date birthdate) {
        this.birthdate = birthdate;
        if (birthdate != null && ecg != null) {
            // infer age
            int age = calculateAge(ecg.getDate(), birthdate);
            if (age < 200) {
                setAge(age);
            }
        }
    }

    /**
     * @param weight the weight to set
     */
    public void setWeight(double weight) {
        this.weight = weight;
    }

    /**
     * @param height the height to set
     */
    public void setHeight(double height) {
        this.height = height;
    }

    /**
     * @return the Body Mass Index (BMI) in kg/m^2.
     */
    public double getBodyMassIndex() {
        if (height == 0)
            return 0;
        return weight / (height * height);
    }

    /**
     * Construct from binary stream.
     *
     * @param is
     * @param ecg
     * @param version
     * @throws IOException
     */
    public SubjectInfo(DataInputStream is, Ecg ecg, long version) throws IOException {
        sex = SubjectSex.readFromBinaryStream(is);
        long t = is.readLong();
        if (t == -1) // invalid date
            birthdate = null;
        else
            birthdate = new Date(t);
        if (version > 8)
            age = is.readInt();
        weight = is.readDouble();
        height = is.readDouble();
        notes = is.readUTF();

        if (age <= 0)
            age = calculateAge(ecg.getDate(), birthdate);
    }

    /**
     * Write to binary stream.
     *
     * @param os
     * @throws IOException
     */
    public void writeToBinaryStream(DataOutputStream os) throws IOException {
        sex.writeToBinaryStream(os);
        if (birthdate != null)
            os.writeLong(birthdate.getTime());
        else
            os.writeLong(-1);
        os.writeInt(age);
        os.writeDouble(weight);
        os.writeDouble(height);
        os.writeUTF(notes);
    }

    /**
     * @return a machine readable string containing all the subject information of this object.
     */
    @Override
    public String toString() {
        String formattedDate = "n/a";
        if (birthdate != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HHmmssZ", Locale.US);
            formattedDate = sdf.format(birthdate);
        }

        String info = "Subject Info:\n";
        info += "Sex = " + sex.name() + ";\n";
        info += "Birthdate = " + formattedDate + ";\n";
        info += "Age = " + age + ";\n";
        info += "Weight = " + weight + " kg;\n";
        info += "Height = " + height + " m;\n";
        info += "Notes = " + notes;

        return info;
    }
}
