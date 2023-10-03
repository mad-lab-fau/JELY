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

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Stroke;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTree;
import javax.swing.TransferHandler;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYLineAnnotation;
import org.jfree.chart.annotations.XYTextAnnotation;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.Range;
import org.jfree.data.xy.DefaultXYDataset;

import de.fau.mad.jely.Ecg;
import de.fau.mad.jely.EcgLead;
import de.fau.mad.jely.EcgSignal;
import de.fau.mad.jely.LeadConfiguration;
import de.fau.mad.jely.annotations.AnnotationManager;
import de.fau.mad.jely.detectors.ElgendiFastQrsDetector;
import de.fau.mad.jely.filter.NallathambiPreprocessing;
import de.fau.mad.jely.io.BinaryEcgFile;
import de.fau.mad.jely.io.FileLoader;

/**
 * An Java based ECG signal viewer and annotation tool relying on the JELY.
 * @author Stefan Gradl
 */
public class EcgEditor {

    private JFrame frmEcgeditor;

    private Ecg loadedEcg = null;
    private JFreeChart ecgChart;
    private ChartPanel ecgChartPanel;

    private JTree leftInfoTree;
    private DefaultMutableTreeNode treeInfo;
    private JTextArea txtLeftDescription;

    /**
     * Launch the application.
     */
    public static void main(String[] args) {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    EcgEditor window = new EcgEditor();
                    window.frmEcgeditor.setVisible(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * Create the application.
     */
    public EcgEditor() {
        initialize();
    }

//    private static XYDataset createDataset() {
//
//        DefaultXYDataset ds = new DefaultXYDataset();
//
//        double[][] data = { {0.1, 0.2, 0.3}, {1, 2, 3} };
//
//        ds.addSeries("series1", data);
//
//        return ds;
//    }


    /**
     * Handles drag & drop actions.
     */
    private TransferHandler handler = new TransferHandler() {
        public boolean canImport(TransferHandler.TransferSupport support) {
            if (!support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                return false;
            }

            /*if (copyItem.isSelected()) {
                boolean copySupported = (COPY & support.getSourceDropActions()) == COPY;

                if (!copySupported) {
                    return false;
                }

                support.setDropAction(COPY);
            }*/

            return true;
        }

        public boolean importData(TransferHandler.TransferSupport support) {
            if (!canImport(support)) {
                return false;
            }

            Transferable t = support.getTransferable();

            try {
                java.util.List<File> l =
                        (java.util.List<File>) t.getTransferData(DataFlavor.javaFileListFlavor);

                if (l.size() > 1) {
                    DlgBatchFiles dlgFiles = new DlgBatchFiles(l);
                    dlgFiles.setVisible(true);
                } else {
                    File f = l.get(0);
                    System.out.println("Data transfer file: " + f.getAbsolutePath());
                    loadFile(f.getAbsolutePath());
                }

            } catch (UnsupportedFlavorException e) {
                return false;
            } catch (IOException e) {
                return false;
            }

            return true;
        }
    };


    /**
     * Loads the given file as an ECG into the UI and displays the signal
     *
     * @param path
     */
    private void loadFile(String path) {
        treeInfo.removeAllChildren();

        long startTime = System.currentTimeMillis();
        loadedEcg = FileLoader.LoadKnownEcgFile(path);
        long totalTime = System.currentTimeMillis() - startTime;
        if (loadedEcg == null) {
            JOptionPane.showMessageDialog(frmEcgeditor, "Unknown file format or error trying to read file.");
            return;
        }
        System.out.println("File " + path + " loaded in " + totalTime + " ms");

        File f = new File(path);
        treeInfo.add(new DefaultMutableTreeNode("Name: " + f.getName()));
        treeInfo.add(new DefaultMutableTreeNode("Type: " + loadedEcg.getClass().getSimpleName()));

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd @ HH:mm:ss Z", Locale.US);
        Date date = loadedEcg.getDate();
        if (date != null) {
            String formattedDate = sdf.format(date);
            treeInfo.add(new DefaultMutableTreeNode("Date: " + formattedDate));
        } else {
            treeInfo.add(new DefaultMutableTreeNode("Date: n/a"));
        }
        //treeInfoDate.setUserObject( "Date: " + formattedDate );
        //treeInfoSampling.setUserObject( "Sampling Rate: " + loadedEcg.getSamplingRate() + " Hz" );
        treeInfo.add(new DefaultMutableTreeNode("Sampling Rate: " + loadedEcg.getSamplingRate() + " Hz"));
        treeInfo.add(new DefaultMutableTreeNode("ECG length: " + loadedEcg.getSignalFromIndex(0).size() + " Samples"));
        treeInfo.add(new DefaultMutableTreeNode("ECG duration: " + loadedEcg.getFormattedSampleTime(loadedEcg.getSignalFromIndex(0).size())));

        DefaultMutableTreeNode treeSubject = new DefaultMutableTreeNode("Subject");
        treeInfo.add(treeSubject);
        treeSubject.add(new DefaultMutableTreeNode("Sex: " + loadedEcg.getSubject().getSex()));
        treeSubject.add(new DefaultMutableTreeNode("Age: " + loadedEcg.getSubject().getAge()));
        treeSubject.add(new DefaultMutableTreeNode("Weight: " + loadedEcg.getSubject().getWeight() + " kg"));
        treeSubject.add(new DefaultMutableTreeNode("Height: " + loadedEcg.getSubject().getHeight() + " m"));
        treeSubject.add(new DefaultMutableTreeNode("Notes: " + loadedEcg.getSubject().getNotes()));

        //treeLeadsRoot.removeAllChildren();
        DefaultMutableTreeNode treeLeadsRoot = new DefaultMutableTreeNode("Leads");
        treeInfo.add(treeLeadsRoot);
        LeadConfiguration ecgLeads = loadedEcg.getLeads();
        for (int i = 0; i < ecgLeads.size(); i++) {
            treeLeadsRoot.add(new DefaultMutableTreeNode(ecgLeads.getLead(i)));
        }

        //treeAnnRoot.removeAllChildren();
        DefaultMutableTreeNode treeAnnRoot = new DefaultMutableTreeNode("Annotations");
        treeInfo.add(treeAnnRoot);
        AnnotationManager an = loadedEcg.getAnnotations();
        for (int i = 0; i < an.size(); i++) {
            treeAnnRoot.add(new DefaultMutableTreeNode(an.getAnnotation(i).getDescription()));
        }

        leftInfoTree.updateUI();
        leftInfoTree.expandRow(0);


        displaySignal();
    }

    private void displaySignal() {
        double[] signal;
        if (loadedEcg.getSignalFromIndex(0).size() < 1e5)
            signal = loadedEcg.getSignalFromIndex(0).toDoubleArray();
        else
            signal = loadedEcg.getSignalFromIndex(0).toDoubleArray(0, (int) 1e5);
        displaySignal(signal, "Lead " + loadedEcg.getLeads().getLead(0));
    }

    private void displaySignal(double[] signal, String legendEntry) {
        DefaultXYDataset ds = new DefaultXYDataset();
        double[] sampleIdx = new double[signal.length];
        for (int i = 0; i < sampleIdx.length; i++) {
            sampleIdx[i] = i;
        }
        double[][] data = {sampleIdx, signal};
        ds.addSeries(legendEntry, data);
        ecgChart = ChartFactory.createXYLineChart("ECG", "Sample", "Amplitude", ds, PlotOrientation.VERTICAL, true, true, false);
        ecgChartPanel.setChart(ecgChart);
    }


    private void addAutomaticAnnotationsRPeaksElgendi() {
        ElgendiFastQrsDetector det = new ElgendiFastQrsDetector(loadedEcg);
        EcgSignal signal = loadedEcg.getSignalFromLead(EcgLead.II);
        if (signal == null) {
            JOptionPane.showMessageDialog(frmEcgeditor, "This loaded ECG does not have a lead II signal!");
            return;
        }
        int[] rPeaks = det.findRPeaks();

        Stroke stroke = new BasicStroke(1f);
        ValueAxis va = ecgChart.getXYPlot().getRangeAxis();
        Range r = va.getRange();
        //System.out.println( "Range: " + r.getLength() );
        double height = r.getLength();
        Color c = new Color(0.3f, 0.3f, 0.8f, 0.5f);

        //Font font = new Font( null, Font.BOLD, 16 );
        for (int i = 0; i < rPeaks.length; i++) {
            //chart.getXYPlot().addAnnotation( new XYBoxAnnotation( rPeaks[i]-20, signal[rPeaks[i]]-height, rPeaks[i]+20, signal[rPeaks[i]]+height, stroke, Color.BLUE, Color.LIGHT_GRAY ) );
            ecgChart.getXYPlot().addAnnotation(new XYLineAnnotation(rPeaks[i], signal.get(rPeaks[i]) - height, rPeaks[i], signal.get(rPeaks[i]) + height, stroke, c));
            XYTextAnnotation ta = new XYTextAnnotation("N", rPeaks[i], signal.get(rPeaks[i]));
            //ta.setFont( font );
            ecgChart.getXYPlot().addAnnotation(ta);
        }
    }


    private void deleteAllAnnotations() {
        ecgChart.getXYPlot().clearAnnotations();
    }


    private void saveAs() {
        JFileChooser fc = new JFileChooser();
        if (fc.showSaveDialog(frmEcgeditor) == JFileChooser.APPROVE_OPTION) {
            BinaryEcgFile.saveEcgToFile(loadedEcg, fc.getSelectedFile().getAbsolutePath());
        }
    }


    /**
     * Initialize the contents of the frame.
     */
    private void initialize() {
        frmEcgeditor = new JFrame();
        frmEcgeditor.setTitle("EcgEditor");
        frmEcgeditor.setBounds(100, 100, 568, 375);
        frmEcgeditor.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frmEcgeditor.getContentPane().setLayout(new BorderLayout(0, 0));
        frmEcgeditor.setTransferHandler(handler);

        JSplitPane splitPane = new JSplitPane();
        frmEcgeditor.getContentPane().add(splitPane, BorderLayout.CENTER);

        //XYDataset ds = createDataset();
        JFreeChart chart = ChartFactory.createXYLineChart("No ECG loaded",
                "...", "...", null, PlotOrientation.VERTICAL, true, true,
                false);
        ecgChartPanel = new ChartPanel(chart);
        splitPane.setRightComponent(ecgChartPanel);


        leftInfoTree = new JTree();
        leftInfoTree.addTreeSelectionListener(new TreeSelectionListener() {
            public void valueChanged(TreeSelectionEvent e) {
                //System.out.println( e.getPath() );
                txtLeftDescription.setText(e.getPath().getLastPathComponent().toString());
            }
        });
        leftInfoTree.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                int selRow = leftInfoTree.getRowForLocation(e.getX(), e.getY());
                TreePath selPath = leftInfoTree.getPathForLocation(e.getX(), e.getY());
                if (selRow != -1) {
                    if (e.getClickCount() == 1) {
                        //mySingleClick(selRow, selPath);
                        //System.out.println("-- single click --");
                    } else if (e.getClickCount() == 2) {
                        //myDoubleClick(selRow, selPath);
                        //DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) ((DefaultMutableTreeNode)selPath.getLastPathComponent()).getUserObject();
                        String selectedNode = (String) ((DefaultMutableTreeNode) selPath.getLastPathComponent()).getUserObject();
                        //System.out.println("-- double click -- id= " + selectedNode );
                    }
                }
            }
        });
        treeInfo = new DefaultMutableTreeNode("ECG");
        leftInfoTree.setModel(new DefaultTreeModel(treeInfo));
//        	new DefaultMutableTreeNode("ECG") {
//        		{
//        			DefaultMutableTreeNode node_1;
//        			treeInfoDate = new DefaultMutableTreeNode("Date: ?"); 
//        			add(treeInfoDate);
//        			treeInfoSampling = new DefaultMutableTreeNode("Sampling Rate: ?");
//        			add(treeInfoSampling);
//        			treeLeadsRoot = new DefaultMutableTreeNode("Leads");        				
//        			add(treeLeadsRoot);
//        			treeAnnRoot = new DefaultMutableTreeNode("Annotations");        				
//        			add(treeAnnRoot);
//        		}
//        	}
//        ));
        leftInfoTree.setMinimumSize(new Dimension(200, 200));

        JSplitPane splitPaneLeft = new JSplitPane();
        splitPaneLeft.setResizeWeight(0.8);
        splitPaneLeft.setOrientation(JSplitPane.VERTICAL_SPLIT);
        splitPaneLeft.setLeftComponent(leftInfoTree);

        splitPane.setLeftComponent(splitPaneLeft);

        txtLeftDescription = new JTextArea();
        txtLeftDescription.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(txtLeftDescription);
        splitPaneLeft.setRightComponent(scrollPane);


        JMenuBar menuBar = new JMenuBar();
        frmEcgeditor.setJMenuBar(menuBar);

        JMenu mnFile = new JMenu("File");
        menuBar.add(mnFile);

        JMenuItem mntmOpen = new JMenuItem("Open...");
        mntmOpen.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JFileChooser fc = new JFileChooser();
                if (fc.showOpenDialog(frmEcgeditor) == JFileChooser.APPROVE_OPTION) {
                    loadFile(fc.getSelectedFile().getAbsolutePath());
                }
            }
        });
        mnFile.add(mntmOpen);

        JSeparator separator = new JSeparator();
        mnFile.add(separator);

        JMenuItem mntmSave = new JMenuItem("Save");
        mntmSave.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (loadedEcg instanceof BinaryEcgFile) {
                    ((BinaryEcgFile) loadedEcg).saveToOriginalFile();
                } else {
                    saveAs();
                }
            }
        });
        mnFile.add(mntmSave);

        JMenuItem mntmSaveAs = new JMenuItem("Save As...");
        mntmSaveAs.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                saveAs();
            }
        });
        mnFile.add(mntmSaveAs);

        JMenuItem mntmAnonymize = new JMenuItem("Anonymize...");
        mnFile.add(mntmAnonymize);

        JSeparator separator_4 = new JSeparator();
        mnFile.add(separator_4);

        JMenuItem mntmExit = new JMenuItem("Exit");
        mntmExit.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                frmEcgeditor.dispose();
            }
        });
        mnFile.add(mntmExit);

        JMenu mnEdit = new JMenu("Edit");
        menuBar.add(mnEdit);

        JMenuItem mntmEnableEcgEditing = new JMenuItem("Enable ECG Editing");
        mntmEnableEcgEditing.setEnabled(false);
        mnEdit.add(mntmEnableEcgEditing);

        JSeparator separator_1 = new JSeparator();
        mnEdit.add(separator_1);

        JMenuItem mntmTrimEcg = new JMenuItem("Trim ECG...");
        mntmTrimEcg.setEnabled(false);
        mnEdit.add(mntmTrimEcg);

        JMenuItem mntmSubjectInformation = new JMenuItem("Subject Information...");
        mnEdit.add(mntmSubjectInformation);
        mntmSubjectInformation.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                DlgSubjectInfoEditor dlg = new DlgSubjectInfoEditor(loadedEcg.getSubject());
                dlg.setModal(true);
                dlg.setVisible(true);
            }
        });

        JMenu mnSignalProcessing = new JMenu("Signal Processing");
        menuBar.add(mnSignalProcessing);

        JMenu mnApplyFilter = new JMenu("Apply Filter");
        mnSignalProcessing.add(mnApplyFilter);

        JMenuItem mntmNallathambiPreprocessing = new JMenuItem("Nallathambi Preprocessing");
        mntmNallathambiPreprocessing.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                EcgSignal sig = loadedEcg.getSignalFromIndex(0).applyFilter(new NallathambiPreprocessing(loadedEcg.getSamplingRate()));
                displaySignal(sig.toDoubleArray(), "Filtered Signal");
            }
        });
        mnApplyFilter.add(mntmNallathambiPreprocessing);

        JMenu mnAnnotations = new JMenu("Annotations");
        menuBar.add(mnAnnotations);

        JMenuItem mntmEnableLiveAnnotation = new JMenuItem("Enable Live Annotation");
        mntmEnableLiveAnnotation.setEnabled(false);
        mnAnnotations.add(mntmEnableLiveAnnotation);

        JSeparator separator_2 = new JSeparator();
        mnAnnotations.add(separator_2);

        JMenuItem mntmAddAnnotation = new JMenuItem("Add Annotation...");
        mntmAddAnnotation.setEnabled(false);
        mnAnnotations.add(mntmAddAnnotation);

        JMenuItem mntmImportAnnotations = new JMenuItem("Import Annotations...");
        mntmImportAnnotations.setEnabled(false);
        mnAnnotations.add(mntmImportAnnotations);

        JMenu mnAutomaticAnnotations = new JMenu("Automatic Annotations");
        mnAnnotations.add(mnAutomaticAnnotations);

        JMenu mnRpeaks = new JMenu("R-Peaks");
        mnAutomaticAnnotations.add(mnRpeaks);

        JMenuItem mntmElgendifastqrsdetector = new JMenuItem("ElgendiFastQrsDetector");
        mntmElgendifastqrsdetector.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                addAutomaticAnnotationsRPeaksElgendi();
            }
        });
        mnRpeaks.add(mntmElgendifastqrsdetector);

        JSeparator separator_3 = new JSeparator();
        mnAnnotations.add(separator_3);

        JMenuItem mntmDeleteAllAnnotations = new JMenuItem("Delete All Annotations");
        mntmDeleteAllAnnotations.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                deleteAllAnnotations();
            }
        });
        mnAnnotations.add(mntmDeleteAllAnnotations);

        JMenu mnHelp = new JMenu("Help");
        menuBar.add(mnHelp);

        JMenuItem mntmAbout = new JMenuItem("About...");
        mntmAbout.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                DlgAbout dlg = new DlgAbout();
                dlg.setVisible(true);
            }
        });
        mnHelp.add(mntmAbout);
    }
}
