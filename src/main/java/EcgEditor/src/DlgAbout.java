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

import java.awt.GridBagLayout;

import javax.swing.JLabel;

import java.awt.GridBagConstraints;
import java.awt.Font;
import java.awt.Insets;

import javax.swing.SpringLayout;
import javax.swing.SwingConstants;

/**
 * @author Stefan Gradl
 */
public class DlgAbout extends JDialog {

    private final JPanel contentPanel = new JPanel();
    private JLabel lblEcgeditor;
    private JLabel lblDigitalSportsGroup;

    /**
     * Launch the application.
     */
    public static void main(String[] args) {
        try {
            DlgAbout dialog = new DlgAbout();
            dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
            dialog.setVisible(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Create the dialog.
     */
    public DlgAbout() {
        setBounds(100, 100, 450, 300);
        getContentPane().setLayout(new BorderLayout());
        contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
        getContentPane().add(contentPanel, BorderLayout.CENTER);
        SpringLayout sl_contentPanel = new SpringLayout();
        contentPanel.setLayout(sl_contentPanel);
        {
            lblEcgeditor = new JLabel("EcgEditor");
            sl_contentPanel.putConstraint(SpringLayout.SOUTH, lblEcgeditor, -137, SpringLayout.SOUTH, contentPanel);
            lblEcgeditor.setHorizontalAlignment(SwingConstants.CENTER);
            sl_contentPanel.putConstraint(SpringLayout.WEST, lblEcgeditor, 5, SpringLayout.WEST, contentPanel);
            sl_contentPanel.putConstraint(SpringLayout.EAST, lblEcgeditor, -15, SpringLayout.EAST, contentPanel);
            lblEcgeditor.setFont(new Font("Tahoma", Font.BOLD, 14));
            contentPanel.add(lblEcgeditor);
        }
        {
            lblDigitalSportsGroup = new JLabel("MaD Lab, Friedrich-Alexander University Erlangen-N\u00FCrnberg");
            lblDigitalSportsGroup.setHorizontalAlignment(SwingConstants.CENTER);
            sl_contentPanel.putConstraint(SpringLayout.NORTH, lblDigitalSportsGroup, 6, SpringLayout.SOUTH, lblEcgeditor);
            sl_contentPanel.putConstraint(SpringLayout.WEST, lblDigitalSportsGroup, 10, SpringLayout.WEST, lblEcgeditor);
            sl_contentPanel.putConstraint(SpringLayout.EAST, lblDigitalSportsGroup, -15, SpringLayout.EAST, contentPanel);
            contentPanel.add(lblDigitalSportsGroup);
        }

        JLabel lblContactStefangradlfaude = new JLabel("Contact: stefan.gradl@fau.de");
        sl_contentPanel.putConstraint(SpringLayout.NORTH, lblContactStefangradlfaude, 31, SpringLayout.SOUTH, lblDigitalSportsGroup);
        sl_contentPanel.putConstraint(SpringLayout.EAST, lblContactStefangradlfaude, -134, SpringLayout.EAST, contentPanel);
        contentPanel.add(lblContactStefangradlfaude);
        {
            JPanel buttonPane = new JPanel();
            buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
            getContentPane().add(buttonPane, BorderLayout.SOUTH);
            {
                JButton okButton = new JButton("OK");
                okButton.setActionCommand("OK");
                buttonPane.add(okButton);
                getRootPane().setDefaultButton(okButton);
            }
            {
                JButton cancelButton = new JButton("Cancel");
                cancelButton.setActionCommand("Cancel");
                buttonPane.add(cancelButton);
            }
        }
    }
}
