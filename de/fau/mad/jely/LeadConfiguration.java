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
package de.fau.mad.jely;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Represents a configuration of different ECG leads.
 *
 * @author Stefan Gradl
 */
public class LeadConfiguration {
    public static final LeadConfiguration SINGLE_UNKNOWN_LEAD = new LeadConfiguration(EcgLead.UNKNOWN);
    public static final LeadConfiguration ONLY_LEAD_II = new LeadConfiguration(EcgLead.II);
    public static final LeadConfiguration LEAD_II_AND_III = new LeadConfiguration(EcgLead.II, EcgLead.III);

    private ArrayList<EcgLead> leads = null;

    /**
     * Whether this lead configuration can not be modified anymore.
     */
    private boolean isFinal = false;

    /**
     * Constructs an empty lead configuration.
     */
    public LeadConfiguration() {
        leads = new ArrayList<EcgLead>();
    }

    /**
     * Constructs a config which is read from the given binary input stream.
     *
     * @param is
     * @throws IOException
     */
    public LeadConfiguration(DataInputStream is) throws IOException {
        int numLeads = is.readInt();
        leads = new ArrayList<EcgLead>(numLeads);
        for (int i = 0; i < numLeads; i++) {
            leads.add(EcgLead.readFromBinaryStream(is));
        }
    }

    /**
     * Copy constructor.
     *
     * @param leads
     */
    public LeadConfiguration(LeadConfiguration leads) {
        this.leads = new ArrayList<EcgLead>(leads.getLeads());

    }

    /**
     * Constructor config with the given number of unknown leads.
     *
     * @param numUnknownLeads
     */
    public LeadConfiguration(int numUnknownLeads) {
        leads = new ArrayList<EcgLead>();
        for (int i = 0; i < numUnknownLeads; i++) {
            add(EcgLead.UNKNOWN);
        }
    }

    /**
     * Constructs a one-lead configuration with the given lead.
     *
     * @param lead
     */
    public LeadConfiguration(EcgLead lead) {
        leads = new ArrayList<EcgLead>(1);
        leads.add(lead);
        isFinal = true;
    }

    /**
     * Constructs a two-lead configuration with the given leads.
     *
     * @param lead1
     * @param lead2
     */
    public LeadConfiguration(EcgLead lead1, EcgLead lead2) {
        leads = new ArrayList<EcgLead>(2);
        leads.add(lead1);
        leads.add(lead2);
        isFinal = true;
    }

    /**
     * Adds the given lead to this configuration.
     *
     * @param lead Lead to add.
     */
    public void add(EcgLead lead) {
        if (isFinal)
            return;

        leads.add(lead);
    }

    /**
     * Returns the number of leads in this configuration.
     *
     * @return Number of leads.
     */
    public int size() {
        return leads.size();
    }

    /**
     * Returns whether this configuration contains the given lead.
     *
     * @param lead
     * @return
     */
    public boolean hasLead(EcgLead lead) {
        return leads.contains(lead);
    }

    /**
     * Returns the lead at the given index.
     *
     * @param index
     * @return
     */
    public EcgLead getLead(int index) {
        return leads.get(index);
    }

    /**
     * Returns the index of the given lead in this configuration.
     *
     * @param lead
     * @return The index of the given lead in this configuration, or -1 if the
     * lead is not contained.
     */
    public int getLeadIndex(EcgLead lead) {
        for (int i = 0; i < leads.size(); i++) {
            if (leads.get(i) == lead) {
                return i;
            }
        }
        return -1;
    }

    private ArrayList<EcgLead> getLeads() {
        return leads;
    }

    public void writeToBinaryStream(DataOutputStream os) throws IOException {
        os.writeInt(leads.size());
        for (int i = 0; i < leads.size(); i++) {
            leads.get(i).writeToBinaryStream(os);
        }
    }

    @Override
    public String toString() {
        return "LeadConfiguration [leads=" + leads + ", isFinal=" + isFinal + "]";
    }


}
