package com.mskrzynski.voicecontrolpcserver;

import javax.swing.table.AbstractTableModel;
import java.util.List;

public class CommandsTable extends AbstractTableModel {

    private List<Commands> commandsList;

    CommandsTable(List<Commands> commandsList) {
        this.commandsList = commandsList;
    }

    private final String[] nazwyKolumn = new String[]{
            "Wyraz", "Polecenie"
    };

    private final Class[] klasyKolumn = new Class[] {
            String.class, String.class
    };

    @Override
    public String getColumnName(int kolumna)
    {
        return nazwyKolumn[kolumna];
    }

    @Override
    public Class<?> getColumnClass(int kolumna)
    {
        return klasyKolumn[kolumna];
    }

    @Override
    public int getColumnCount() {
        return nazwyKolumn.length;
    }

    @Override
    public int getRowCount() {
        return commandsList.size();
    }

    @Override
    public Object getValueAt(int wiersz, int kolumna) {
        Commands odpytywany_wiersz = commandsList.get(wiersz);
        if(kolumna == 0) {
            return odpytywany_wiersz.getWyraz();
        }
        else if(kolumna == 1) {
            return odpytywany_wiersz.getPolecenie();
        }
        return null;
    }

    @Override
    public void setValueAt(Object aValue, int wiersz, int kolumna) {
        Commands odpytywany_wiersz = commandsList.get(wiersz);
        if (kolumna == 0) {
            odpytywany_wiersz.setWyraz((String) aValue);
        }
        else if(kolumna == 1)
        {
            odpytywany_wiersz.setPolecenie((String) aValue);
        }
        fireTableCellUpdated(wiersz, kolumna);
    }

    void add() {
        fireTableRowsInserted(commandsList.size() - 1, commandsList.size() - 1);
    }

    void remove(){
        fireTableRowsDeleted(commandsList.size() - 1, commandsList.size() - 1);
    }
}
