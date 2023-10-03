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

import java.awt.BorderLayout;
import java.awt.FlowLayout;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.JComboBox;
import javax.swing.DefaultComboBoxModel;

import de.fau.mad.jely.annotations.SubjectInfo;
import de.fau.mad.jely.annotations.SubjectSex;

import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Properties;

import javax.swing.JFormattedTextField;
import javax.swing.JFormattedTextField.AbstractFormatter;

import java.awt.event.ActionListener;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.awt.event.ActionEvent;

import javax.swing.JTextArea;

import java.awt.Dimension;

import javax.swing.border.SoftBevelBorder;
import javax.swing.border.BevelBorder;
import javax.swing.border.LineBorder;

import java.awt.Color;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
import java.awt.event.InputMethodListener;
import java.awt.event.InputMethodEvent;

import javax.swing.event.CaretListener;
import javax.swing.event.CaretEvent;
import javax.swing.JCheckBox;
import javax.swing.SwingConstants;

import java.awt.event.ItemListener;
import java.awt.event.ItemEvent;

/**
 * @author Stefan Gradl
 */
public class DlgSubjectInfoEditor extends JDialog {

    private final JPanel contentPanel = new JPanel();
    private SubjectInfo subject;

    JComboBox<SubjectSex> cmbSex;
    JFormattedTextField txtBirthdate;
    JCheckBox tglValid;
    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    JFormattedTextField txtAge;
    JFormattedTextField txtWeight;
    JFormattedTextField txtHeight;
    JTextArea txtNotes;

    /**
     * Launch the application.
     */
    public static void main(String[] args) {
        try {
            DlgSubjectInfoEditor dialog = new DlgSubjectInfoEditor(new SubjectInfo(null));
            dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
            dialog.setVisible(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    void updateBirthdateAndAge() {
        try {
            if (txtBirthdate == null || subject == null || txtAge == null || tglValid == null)
                return;

            Date bday = dateFormat.parse(txtBirthdate.getText());
            int age = SubjectInfo.calculateAge(subject.getEcg().getDate(), bday);
            if (age > 200) {
                throw new ParseException("Invalid age.", 0);
            }

            txtAge.setText(Integer.toString(age));
            tglValid.setSelected(true);
        } catch (ParseException e1) {
            tglValid.setSelected(false);
            txtAge.setText("0");
            return;
        }
    }


    /**
     * Create the dialog.
     */
    public DlgSubjectInfoEditor(SubjectInfo s) {
        this.subject = s;

        setTitle("Subject Information");
        setBounds(100, 100, 460, 343);
        getContentPane().setLayout(new BorderLayout());
        contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
        getContentPane().add(contentPanel, BorderLayout.NORTH);
        GridBagLayout gbl_contentPanel = new GridBagLayout();
        gbl_contentPanel.columnWidths = new int[]{165, 22, 77, 0};
        gbl_contentPanel.rowHeights = new int[]{20, 0, 0, 0, 0, 0, 0};
        gbl_contentPanel.columnWeights = new double[]{0.0, 0.0, 1.0, Double.MIN_VALUE};
        gbl_contentPanel.rowWeights = new double[]{0.0, 1.0, 0.0, 0.0, 1.0, 0.0, Double.MIN_VALUE};
        contentPanel.setLayout(gbl_contentPanel);
        {
            JLabel lblSex = new JLabel("Sex:");
            GridBagConstraints gbc_lblSex = new GridBagConstraints();
            gbc_lblSex.anchor = GridBagConstraints.WEST;
            gbc_lblSex.insets = new Insets(0, 0, 5, 5);
            gbc_lblSex.gridx = 0;
            gbc_lblSex.gridy = 0;
            contentPanel.add(lblSex, gbc_lblSex);
        }
        {
            cmbSex = new JComboBox<SubjectSex>();
            cmbSex.setModel(new DefaultComboBoxModel<SubjectSex>(SubjectSex.values()));
            cmbSex.setSelectedItem(subject.getSex());
            GridBagConstraints gbc_comboBox = new GridBagConstraints();
            gbc_comboBox.insets = new Insets(0, 0, 5, 0);
            gbc_comboBox.anchor = GridBagConstraints.NORTHWEST;
            gbc_comboBox.gridx = 2;
            gbc_comboBox.gridy = 0;
            contentPanel.add(cmbSex, gbc_comboBox);
        }
        {
            JLabel lblNewLabel = new JLabel("Date of birth:");
            GridBagConstraints gbc_lblNewLabel = new GridBagConstraints();
            gbc_lblNewLabel.anchor = GridBagConstraints.WEST;
            gbc_lblNewLabel.insets = new Insets(0, 0, 5, 5);
            gbc_lblNewLabel.gridx = 0;
            gbc_lblNewLabel.gridy = 1;
            contentPanel.add(lblNewLabel, gbc_lblNewLabel);
        }
        {
            JPanel panel = new JPanel();
            panel.setBorder(null);
            GridBagConstraints gbc_panel = new GridBagConstraints();
            gbc_panel.fill = GridBagConstraints.VERTICAL;
            gbc_panel.anchor = GridBagConstraints.WEST;
            gbc_panel.insets = new Insets(0, 0, 5, 0);
            gbc_panel.gridx = 2;
            gbc_panel.gridy = 1;
            contentPanel.add(panel, gbc_panel);
            panel.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 0));
            txtBirthdate = new JFormattedTextField(dateFormat);
            if (subject.getBirthdate() != null)
                txtBirthdate.setValue(subject.getBirthdate());
            else
                txtBirthdate.setText("1500-04-24");
            panel.add(txtBirthdate);
            txtBirthdate.setColumns(10);
            {
                tglValid = new JCheckBox("Valid");
                tglValid.setMargin(new Insets(2, 5, 2, 2));
                tglValid.addItemListener(new ItemListener() {
                    public void itemStateChanged(ItemEvent arg0) {
                        if (tglValid.isSelected())
                            txtAge.setEditable(false);
                        else
                            txtAge.setEditable(true);
                    }
                });
                panel.add(tglValid);
            }
            txtBirthdate.addCaretListener(new CaretListener() {
                public void caretUpdate(CaretEvent arg0) {
                    updateBirthdateAndAge();
                }
            });
        }
        {
            JLabel lblAge = new JLabel("Age:");
            GridBagConstraints gbc_lblAge = new GridBagConstraints();
            gbc_lblAge.anchor = GridBagConstraints.WEST;
            gbc_lblAge.insets = new Insets(0, 0, 5, 5);
            gbc_lblAge.gridx = 0;
            gbc_lblAge.gridy = 2;
            contentPanel.add(lblAge, gbc_lblAge);
        }
        {
            txtAge = new JFormattedTextField(NumberFormat.getNumberInstance(Locale.US));
            txtAge.setColumns(5);
            txtAge.setText(Integer.toString(subject.getAge()));
            GridBagConstraints gbc_txtAge = new GridBagConstraints();
            gbc_txtAge.anchor = GridBagConstraints.NORTHWEST;
            gbc_txtAge.insets = new Insets(0, 0, 5, 0);
            gbc_txtAge.gridx = 2;
            gbc_txtAge.gridy = 2;
            contentPanel.add(txtAge, gbc_txtAge);
        }
        {
            JLabel lblWeightkg = new JLabel("Weight (kg):");
            GridBagConstraints gbc_lblWeightkg = new GridBagConstraints();
            gbc_lblWeightkg.anchor = GridBagConstraints.WEST;
            gbc_lblWeightkg.insets = new Insets(0, 0, 5, 5);
            gbc_lblWeightkg.gridx = 0;
            gbc_lblWeightkg.gridy = 3;
            contentPanel.add(lblWeightkg, gbc_lblWeightkg);
        }
        {
            txtWeight = new JFormattedTextField(NumberFormat.getNumberInstance(Locale.US));
            txtWeight.setColumns(5);
            txtWeight.setText(Double.toString(subject.getWeight()));
            GridBagConstraints gbc_txtWeight = new GridBagConstraints();
            gbc_txtWeight.insets = new Insets(0, 0, 5, 0);
            gbc_txtWeight.anchor = GridBagConstraints.WEST;
            gbc_txtWeight.gridx = 2;
            gbc_txtWeight.gridy = 3;
            contentPanel.add(txtWeight, gbc_txtWeight);
        }
        {
            JLabel lblHeightm = new JLabel("Height (m):");
            GridBagConstraints gbc_lblHeightm = new GridBagConstraints();
            gbc_lblHeightm.anchor = GridBagConstraints.WEST;
            gbc_lblHeightm.insets = new Insets(0, 0, 5, 5);
            gbc_lblHeightm.gridx = 0;
            gbc_lblHeightm.gridy = 4;
            contentPanel.add(lblHeightm, gbc_lblHeightm);
        }
        {
            txtHeight = new JFormattedTextField(NumberFormat.getNumberInstance(Locale.US));
            txtHeight.setColumns(5);
            txtHeight.setText(Double.toString(subject.getHeight()));
            GridBagConstraints gbc_txtHeight = new GridBagConstraints();
            gbc_txtHeight.insets = new Insets(0, 0, 5, 0);
            gbc_txtHeight.anchor = GridBagConstraints.WEST;
            gbc_txtHeight.gridx = 2;
            gbc_txtHeight.gridy = 4;
            contentPanel.add(txtHeight, gbc_txtHeight);
        }
        {
            JLabel lblNotesmedicationEtc = new JLabel("Notes (medication, etc.):");
            GridBagConstraints gbc_lblNotesmedicationEtc = new GridBagConstraints();
            gbc_lblNotesmedicationEtc.anchor = GridBagConstraints.NORTHWEST;
            gbc_lblNotesmedicationEtc.insets = new Insets(0, 0, 0, 5);
            gbc_lblNotesmedicationEtc.gridx = 0;
            gbc_lblNotesmedicationEtc.gridy = 5;
            contentPanel.add(lblNotesmedicationEtc, gbc_lblNotesmedicationEtc);
        }
        {
            txtNotes = new JTextArea();
            txtNotes.setBorder(new LineBorder(new Color(0, 0, 0)));
            txtNotes.setRows(5);
            txtNotes.setText(subject.getNotes());
            GridBagConstraints gbc_textArea = new GridBagConstraints();
            gbc_textArea.fill = GridBagConstraints.BOTH;
            gbc_textArea.gridx = 2;
            gbc_textArea.gridy = 5;
            contentPanel.add(txtNotes, gbc_textArea);
        }
        {
            JPanel buttonPane = new JPanel();
            buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
            getContentPane().add(buttonPane, BorderLayout.SOUTH);
            {
                JButton okButton = new JButton("OK");
                okButton.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        subject.setSex((SubjectSex) cmbSex.getSelectedItem());
                        if (tglValid.isSelected()) {
                            try {
                                subject.setBirthdate(dateFormat.parse(txtBirthdate.getText()));
                            } catch (ParseException e1) {
                                // TODO Auto-generated catch block
                                e1.printStackTrace();
                                return;
                            }
                        } else {
                            subject.setBirthdate(null);
                            int age = Integer.parseInt(txtAge.getText());
                            subject.setAge(age);
                        }
                        subject.setWeight(Double.parseDouble(txtWeight.getText()));
                        subject.setHeight(Double.parseDouble(txtHeight.getText()));
                        subject.setNotes(txtNotes.getText());
                        dispose();
                    }
                });
                okButton.setActionCommand("OK");
                buttonPane.add(okButton);
                getRootPane().setDefaultButton(okButton);
            }
            {
                JButton cancelButton = new JButton("Cancel");
                cancelButton.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        dispose();
                    }
                });
                cancelButton.setActionCommand("Cancel");
                buttonPane.add(cancelButton);
            }
        }
    }

}
