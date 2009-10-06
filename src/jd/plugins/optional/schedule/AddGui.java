//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.optional.schedule;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Calendar;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;

import jd.plugins.optional.schedule.modules.SchedulerModuleInterface;
import jd.utils.locale.JDL;
import net.miginfocom.swing.MigLayout;

public class AddGui extends JPanel implements ActionListener, ChangeListener, DocumentListener, MouseListener {
    private static final long serialVersionUID = 8080132393187788526L;
    private MainGui gui;
    private Schedule schedule;
    private MyTableModel tableModel;
    private JTable table;
    private Actions act;
    private JComboBox cboActions;
    private JButton add;
    private JTextField parameter;
    private JButton delete;
    private JTextField name;
    private JRadioButton optDate;
    private JRadioButton optDaily;
    private JSpinner day;
    private JSpinner month;
    private JSpinner year;
    private JSpinner hour;
    private JSpinner minute;
    private JPanel datepre;
    private String oldText;
    private JButton cancel;
    private JButton save;
    private JLabel problems;
    private JRadioButton optHourly;
    private JRadioButton optWeekly;
    private JRadioButton optSpecific;
    private JSpinner repeathour;
    private JSpinner repeatminute;
    private boolean edit;

    public AddGui(Schedule schedule, MainGui gui, Actions act, boolean edit) {
        this.schedule = schedule;
        this.gui = gui;
        this.act = act;

        oldText = act.getName();

        this.edit = edit;

        setLayout(new MigLayout("wrap 1, fill", "[fill, grow]"));

        JPanel main = new JPanel(new MigLayout("ins 5, wrap 1", "[fill, grow]", "[fill, grow][]"));

        JPanel date = new JPanel();
        date.setLayout(new MigLayout("ins 5, wrap 2", "[fill, grow]10[fill, grow]"));
        date.setBorder(BorderFactory.createLineBorder(getBackground().darker()));

        date.add(new JLabel(JDL.L("plugin.optional.scheduler.add.name", "Name")));

        name = new JTextField(act.getName());
        name.getDocument().addDocumentListener(this);
        date.add(name, "w 20%");

        JPanel repeats = new JPanel();
        repeats.setLayout(new MigLayout("wrap 2", "[fill, grow][fill, grow]"));

        optDate = new JRadioButton(JDL.L("plugin.optional.scheduler.add.once", "Only once"));
        optDate.setSelected(true);
        repeats.add(optDate, "split 4");

        optHourly = new JRadioButton(JDL.L("plugin.optional.scheduler.add.hourly", "Hourly"));
        repeats.add(optHourly, "");

        optDaily = new JRadioButton(JDL.L("plugin.optional.scheduler.add.daily", "Daily"));
        repeats.add(optDaily, "");

        optWeekly = new JRadioButton(JDL.L("plugin.optional.scheduler.add.weekly", "Weekly"));
        repeats.add(optWeekly, "");

        optSpecific = new JRadioButton(JDL.L("plugin.optional.scheduler.add.specific", "Choose interval"));
        optSpecific.addChangeListener(this);
        repeats.add(optSpecific, "newline");

        JPanel specpre = new JPanel();
        specpre.setLayout(new MigLayout("", "[left]"));

        specpre.add(new JLabel(JDL.L("plugin.optional.scheduler.add.hour", "Hour: ")));

        repeathour = new JSpinner();
        repeathour.setModel(new SpinnerNumberModel(01, 00, 23, 1));
        repeathour.setEnabled(false);
        specpre.add(repeathour);

        specpre.add(new JLabel(JDL.L("plugin.optional.scheduler.add.minute", "Minute: ")));

        repeatminute = new JSpinner();
        repeatminute.setModel(new SpinnerNumberModel(00, 00, 59, 1));
        repeatminute.setEnabled(false);
        specpre.add(repeatminute);

        repeats.add(specpre);

        ButtonGroup grp = new ButtonGroup();
        grp.add(optDate);
        grp.add(optHourly);
        grp.add(optDaily);
        grp.add(optWeekly);
        grp.add(optSpecific);

        date.add(new JLabel(JDL.L("plugin.optional.scheduler.add.repeats", "Repeats")));
        date.add(repeats);

        datepre = new JPanel();
        datepre.setLayout(new MigLayout("ins 0", "[][]10[][]10[][]"));
        datepre.add(new JLabel(JDL.L("plugin.optional.scheduler.add.day", "Day: ")));

        day = new JSpinner();
        day.setModel(new SpinnerNumberModel(Calendar.getInstance().get(Calendar.DAY_OF_MONTH), 1, 31, 1));
        datepre.add(day, "sizegroup spinner");

        datepre.add(new JLabel(JDL.L("plugin.optional.scheduler.add.month", "Month: ")));

        month = new JSpinner();
        month.setModel(new SpinnerNumberModel(Calendar.getInstance().get(Calendar.MONTH) + 1, 1, 12, 1));
        datepre.add(month, "sizegroup spinner");

        datepre.add(new JLabel(JDL.L("plugin.optional.scheduler.add.year", "Year: ")));

        year = new JSpinner();
        year.setModel(new SpinnerNumberModel(Calendar.getInstance().get(Calendar.YEAR), 2009, 2015, 1));
        datepre.add(year, "sizegroup spinner");

        datepre.add(new JLabel(JDL.L("plugin.optional.scheduler.add.hour", "Hour: ")), "newline");

        hour = new JSpinner();
        hour.setModel(new SpinnerNumberModel(Calendar.getInstance().get(Calendar.HOUR_OF_DAY), 00, 23, 1));
        datepre.add(hour, "sizegroup spinner");

        datepre.add(new JLabel(JDL.L("plugin.optional.scheduler.add.minute", "Minute: ")));

        minute = new JSpinner();
        minute.setModel(new SpinnerNumberModel(Calendar.getInstance().get(Calendar.MINUTE), 00, 59, 1));
        datepre.add(minute, "sizegroup spinner");

        date.add(new JLabel(JDL.L("plugin.optional.scheduler.add.date", "Date")));
        date.add(datepre, "spany 2");
        date.add(new JLabel(JDL.L("plugin.optional.scheduler.add.time", "Time")));

        JPanel actions = new JPanel();
        actions.setLayout(new MigLayout("ins 5, wrap 2", "[fill, grow]10[fill, grow]"));
        actions.setBorder(BorderFactory.createLineBorder(getBackground().darker()));

        cboActions = new JComboBox();
        cboActions.addActionListener(this);
        actions.add(cboActions, "w 80%");

        add = new JButton("+");
        add.addActionListener(this);
        actions.add(add);

        parameter = new JTextField();
        parameter.setEnabled(false);
        actions.add(parameter);

        delete = new JButton("-");
        delete.setEnabled(false);
        delete.addActionListener(this);
        actions.add(delete);

        tableModel = new MyTableModel();
        table = new JTable(tableModel);
        table.addMouseListener(this);

        JPanel control = new JPanel(new MigLayout("wrap 3", "[grow, fill, right][right][right]"));

        problems = new JLabel();
        problems.setForeground(Color.RED);
        control.add(problems);

        cancel = new JButton(JDL.L("plugin.optional.scheduler.add.cancel", "Cancel"));
        cancel.addActionListener(this);
        control.add(cancel, "align right,tag cancel");

        save = new JButton(JDL.L("plugin.optional.scheduler.add.save", "Save"));
        save.addActionListener(this);
        control.add(save, "align right,tag save");

        if (edit) {
            Calendar c = Calendar.getInstance();
            c.setTime(act.getDate());
            day.setValue(c.get(Calendar.DAY_OF_MONTH));
            month.setValue(c.get(Calendar.MONTH) + 1);
            year.setValue(c.get(Calendar.YEAR));
            hour.setValue(c.get(Calendar.HOUR_OF_DAY));
            minute.setValue(c.get(Calendar.MINUTE));

            switch (act.getRepeat()) {
            case 0:
                optDate.setSelected(true);
                break;
            case 60:
                optHourly.setSelected(true);
                break;
            case 1440:
                optDaily.setSelected(true);
                break;
            case 10080:
                optWeekly.setSelected(true);
                break;
            default:
                optSpecific.setSelected(true);
                int hour = act.getRepeat() / 60;
                repeathour.setValue(hour);
                repeatminute.setValue(act.getRepeat() - (hour * 60));
            }

            tableModel.fireTableRowsInserted(0, act.getExecutions().size());
        }

        fillComboBox();

        main.add(date);
        main.add(actions);
        main.add(new JScrollPane(table), "hmin 100");
        main.add(control);

        add(main);
    }

    private class MyTableModel extends AbstractTableModel {
        private static final long serialVersionUID = -2404339596786592942L;

        @Override
        public String getColumnName(int column) {
            switch (column) {
            case 0:
                return JDL.L("plugin.optional.scheduler.add.column.executions.name", "Name");
            case 1:
                return JDL.L("plugin.optional.scheduler.add.column.executions.parameter", "Parameter");
            }
            return super.getColumnName(column);
        }

        public int getColumnCount() {
            return 2;
        }

        public int getRowCount() {
            return act.getExecutions().size();
        }

        public Object getValueAt(int rowIndex, int columnIndex) {
            switch (columnIndex) {
            case 0:
                return act.getExecutions().get(rowIndex).getModule().getTranslation();
            case 1:
                return act.getExecutions().get(rowIndex).getParameter();
            }

            return null;
        }
    }

    public void actionPerformed(ActionEvent e) {
        problems.setText("");

        if (e.getSource() == save) {
            if (name.getText().equals("")) {
                problems.setText(JDL.L("plugin.optional.scheduler.add.problem.emptyname", "Name is empty"));
                return;
            } else if (act.getExecutions().size() == 0) {
                problems.setText(JDL.L("plugin.optional.scheduler.add.problem.nochanges", "No changes made"));
                return;
            }

            act.setName(name.getText());

            Calendar c = Calendar.getInstance();
            c.set(Calendar.YEAR, (Integer) year.getValue());
            c.set(Calendar.MONTH, (Integer) month.getValue() - 1);
            c.set(Calendar.DAY_OF_MONTH, (Integer) day.getValue());
            c.set(Calendar.HOUR_OF_DAY, (Integer) hour.getValue());
            c.set(Calendar.MINUTE, (Integer) minute.getValue());
            act.setDate(c.getTime());

            if (optDate.isSelected()) {
                act.setRepeat(0);
            } else if (optHourly.isSelected()) {
                act.setRepeat(60);
            } else if (optDaily.isSelected()) {
                act.setRepeat(1440);
            } else if (optWeekly.isSelected()) {
                act.setRepeat(10080);
            } else if (optSpecific.isSelected()) {
                act.setRepeat(((Integer) repeathour.getValue() * 60) + (Integer) repeatminute.getValue());
            }

            if (edit) {
                gui.updateAction(act);
            } else {
                schedule.addAction(act);
                gui.addAction(act);
            }

            return;
        } else if (e.getSource() == cboActions) {
            for (SchedulerModuleInterface smi : schedule.getModules()) {
                if (smi.getTranslation().equals(cboActions.getSelectedItem())) {
                    if (smi.needParameter())
                        parameter.setText("");
                    else
                        parameter.setText(JDL.L("plugin.optional.scheduler.add.noparameter", "No Parameter needed"));
                    parameter.setEnabled(smi.needParameter());
                    parameter.requestFocus();
                    return;
                }
            }
        } else if (e.getSource() == add) {
            for (SchedulerModuleInterface smi : schedule.getModules()) {
                if (smi.getTranslation().equals(cboActions.getSelectedItem())) {
                    if (smi.needParameter() && !smi.checkParameter(parameter.getText())) {
                        problems.setText(JDL.L("plugin.optional.scheduler.add.problem.badparameter", "No correct Parameter"));
                        return;
                    }
                    if (parameter.getText().equals(JDL.L("plugin.optional.scheduler.add.noparameter", "No Parameter needed")))
                        act.addExecutions(new Executions(smi, ""));
                    else
                        act.addExecutions(new Executions(smi, parameter.getText()));
                    tableModel.fireTableRowsInserted(act.getExecutions().size(), act.getExecutions().size());
                    fillComboBox();
                    return;
                }
            }
        } else if (e.getSource() == delete) {
            act.removeExecution(table.getSelectedRow());
            tableModel.fireTableRowsDeleted(table.getSelectedRow(), table.getSelectedRow());
            delete.setEnabled(false);
            fillComboBox();
        } else if (e.getSource() == cancel) {
            gui.removeTab(act);
        }
    }

    private void fillComboBox() {
        cboActions.removeAllItems();

        for (int i = 0; i < schedule.getModules().size(); i++) {
            boolean found = false;
            for (Executions e : act.getExecutions()) {
                if (e.getModule().getTranslation().equals(schedule.getModules().get(i).getTranslation())) found = true;
            }

            if (!found) cboActions.addItem(schedule.getModules().get(i).getTranslation());
        }
    }

    public void stateChanged(ChangeEvent e) {
        repeathour.setEnabled(optSpecific.isSelected());
        repeatminute.setEnabled(optSpecific.isSelected());
    }

    public void changedUpdate(DocumentEvent e) {
        gui.changeTabText(oldText, name.getText());
        oldText = name.getText();
    }

    public void insertUpdate(DocumentEvent e) {
        changedUpdate(e);
    }

    public void removeUpdate(DocumentEvent e) {
        changedUpdate(e);
    }

    public void mouseClicked(MouseEvent e) {
        if (table.getSelectedRowCount() > 0) {
            delete.setEnabled(true);
        }
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    public void mousePressed(MouseEvent e) {
    }

    public void mouseReleased(MouseEvent e) {
    }
}
