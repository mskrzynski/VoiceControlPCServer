package com.mskrzynski.voicecontrolpcserver;

import com.github.sarxos.webcam.Webcam;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecuteResultHandler;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.lang3.StringUtils;
import javax.imageio.ImageIO;
import javax.persistence.*;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.List;
import java.util.ArrayList;

public class VoiceControlPCServer implements ActionListener{
    private static JFrame frame;
    private JPanel VoiceControlPCServer;
    private JButton button_export;
    private JButton button_import;
    private JButton button_new;
    private JButton button_edit;
    private JButton button_delete;
    private JButton button_deleteDB;
    private JButton button_example;
    private JTextField phonenameTXT;
    private JTextField hostnameTXT;
    private JTextField ipaddressTXT;
    private JTextField statusTXT;
    private JTextField amountTXT;
    private JTextField searchTXT;
    private JTextField publicIP;
    private JTextField podane_polecenie;
    @SuppressWarnings("unused")
    private JLabel label_phonename;
    @SuppressWarnings("unused")
    private JLabel label_status;
    @SuppressWarnings("unused")
    private JLabel label_search;
    @SuppressWarnings("unused")
    private JLabel label_ipaddress;
    @SuppressWarnings("unused")
    private JLabel label_hostname;
    private JTable commandsJTable;
    private List<Commands> commandsList;
    private CommandsTable tabela;
    private TableRowSorter<TableModel> sorter;
    private EntityManager em;
    private Webcam kamera_internetowa;
    private boolean no_webcam = false;

    private VoiceControlPCServer() {
        //usuwanie obramowania, ponieważ edytor nie posiada takich opcji
        amountTXT.setBorder(javax.swing.BorderFactory.createEmptyBorder());
        publicIP.setBorder(javax.swing.BorderFactory.createEmptyBorder());

        //wyśrodkowanie tekstu pola Status
        statusTXT.setHorizontalAlignment(JTextField.CENTER);

        //utwórz tabelę na podstawie listy
        commandsList = new ArrayList<>();
        tabela = new CommandsTable(commandsList);
        commandsJTable.setModel(tabela);
        commandsJTable.getTableHeader().setReorderingAllowed(false); //nie zezwalaj na zmianę kolejności kolumn
        commandsJTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); //zezwalaj tylko na wybranie jednego elementu

        //sortuj domyślnie według pierwszej kolumny
        sorter = new TableRowSorter<>(commandsJTable.getModel());
        commandsJTable.setRowSorter(sorter);
        List<RowSorter.SortKey> sortKeys = new ArrayList<>();
        sortKeys.add(new RowSorter.SortKey(0, SortOrder.ASCENDING));
        sorter.setSortKeys(sortKeys);
        sorter.sort();

        //pobranie lokalniego i publicznego adresu IP, oraz nazwy komputera serwera
        try(DatagramSocket socket = new DatagramSocket())
        {
            hostnameTXT.setText(InetAddress.getLocalHost().getHostName());
            socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
            ipaddressTXT.setText(socket.getLocalAddress().getHostAddress());
            BufferedReader publicIPReader = new BufferedReader(new InputStreamReader(new URL("http://checkip.amazonaws.com").openStream()));
            publicIP.setText("Publiczny adres IP: " + publicIPReader.readLine());

        }
        catch (IOException e) {
            e.printStackTrace();
        }

        //sprawdzenie czy kamera istnieje i jej uruchomienie
        kamera_internetowa = Webcam.getDefault();
        if (kamera_internetowa != null) kamera_internetowa.open();
        else no_webcam = true;

        //ustanawianie trwałego połączenia z bazą danych za pomocą JPA
        EntityManagerFactory factory = Persistence.createEntityManagerFactory( "Commands_persistence-unit" );
        em = factory.createEntityManager();

        //odczytywanie danych z bazy danych do tabeli przy uruchomieniu oraz ustalanie ilości wpisów w tej bazie
        Query[] q = {em.createQuery("SELECT COUNT(*) FROM Commands")};
        Number[] ilosc = {(Number) q[0].getSingleResult()};
        amountTXT.setText("Ilość polecen: " + ilosc[0].toString());

        //dodawanie wartości z bazy danych do tabeli
        if(ilosc[0].intValue() != 0){ //nie dodawaj nic do tabeli gdy baza jest pusta
            Query[] qu = {em.createQuery("SELECT MAX(id) FROM Commands")};
            Number[] max_id = {(Number) qu[0].getSingleResult()};

            em.getTransaction().begin();
            //szukanie do maksymalnego primary key
            for(int i = 1; i <= max_id[0].intValue(); i++){
                Commands command = em.find(Commands.class, i); //i - primary key
                if(command != null){ //omijanie nieistniejących primary key
                    commandsList.add(command);
                    tabela.add();
                }
            }
            em.getTransaction().commit();
        }

        //wyszukiwanie na podstawie tekstu w polu wyszukiwania
        searchTXT.getDocument().addDocumentListener(
                new DocumentListener() {
                    public void changedUpdate(DocumentEvent e) { Filtr(); }
                    public void insertUpdate(DocumentEvent e) { Filtr(); }
                    public void removeUpdate(DocumentEvent e) { Filtr(); }
                });

        //akcje przycisków
        button_deleteDB.addActionListener(
                e -> {
                    int result = JOptionPane.showConfirmDialog(VoiceControlPCServer, "Czy napewno chcesz usunąć całą bazę poleceń?\nOperacja ta jest nieodwracalna!", "Usuwanie bazy poleceń", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                    if (result == 0) {
                        try {
                            //usuwamy bazę danych i tworzymy ją na nowo
                            new SendStatement("DROP TABLE Commands");
                            new SendStatement("CREATE TABLE Commands (" +
                                    "id INT IDENTITY NOT NULL PRIMARY KEY, " +
                                    "wyraz VARCHAR(255) NOT NULL UNIQUE, " +
                                    "polecenie VARCHAR(255) NOT NULL)");

                            JOptionPane.showMessageDialog(VoiceControlPCServer, "Pomyślnie usunięto bazę poleceń\nNależy ponownie uruchomić serwer", "Usuwanie bazy poleceń", JOptionPane.INFORMATION_MESSAGE);
                            System.exit(0);
                        }
                        catch(Exception exc){
                            JOptionPane.showMessageDialog(VoiceControlPCServer, "Błąd przy usuwaniu bazy poleceń, sprawdź czy masz dostęp do pliku bazy danych", "Usuwanie bazy poleceń", JOptionPane.ERROR_MESSAGE);
                            exc.printStackTrace();
                        }
                    }
                }
        );

        button_export.addActionListener(
                e -> {
                    //tworzymy okno zapisu i ustalamy jego właściwości
                    JFileChooser filechooser = new JFileChooser();
                    filechooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
                    filechooser.addChoosableFileFilter(new CSVFilter());
                    filechooser.setAcceptAllFileFilterUsed(false);
                    int returnVal = filechooser.showSaveDialog(VoiceControlPCServer);
                    if (returnVal == JFileChooser.APPROVE_OPTION) {
                        //pobieramy nazwę pliku, dodajemy rozszerzenie .csv gdy potrzebne
                        String nazwa_pliku_csv = filechooser.getSelectedFile().getAbsolutePath();
                        if (!nazwa_pliku_csv.endsWith(".csv")) nazwa_pliku_csv += ".csv";
                        //zapisanie pliku CSV za pomocą wyrażenia
                        try {
                            new SendStatement("CALL CSVWRITE('" + nazwa_pliku_csv + "', 'SELECT * FROM Commands')");
                            JOptionPane.showMessageDialog(VoiceControlPCServer, "Pomyślnie wyeksportowano bazę danych", "Eksport CSV", JOptionPane.INFORMATION_MESSAGE);
                        }
                        catch (Exception ex) {
                            JOptionPane.showMessageDialog(VoiceControlPCServer, "Błąd przy eksportowaniu bazy danych, sprawdź czy masz dostęp do ścieżki zapisu", "Eksport CSV", JOptionPane.ERROR_MESSAGE);
                            ex.printStackTrace();
                        }
                    }
                }
        );

        button_import.addActionListener(
                e -> {
                    //tworzymy okno wczytania i jego właściwości
                    JFileChooser filechooser = new JFileChooser();
                    filechooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
                    filechooser.addChoosableFileFilter(new CSVFilter());
                    filechooser.setAcceptAllFileFilterUsed(false);
                    int returnVal = filechooser.showOpenDialog(VoiceControlPCServer);
                    if (returnVal == JFileChooser.APPROVE_OPTION) {
                        //pobieramy nazwę wybranego pliku
                        String nazwa_pliku_csv = filechooser.getSelectedFile().getAbsolutePath();

                        int result = JOptionPane.showConfirmDialog(VoiceControlPCServer, "Zaimportowanie pliku CSV usunie całą obecną bazę poleceń\nCzy napewno chcesz zaimportować plik CSV?", "Import CSV", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

                        if (result == 0) {
                            try {
                                //utworzenie testowej tymczasowej tabeli do sprawdzenia poprawności pliku CSV
                                new SendStatement("CREATE TABLE Test (" +
                                        "id INT IDENTITY NOT NULL PRIMARY KEY, " +
                                        "wyraz VARCHAR(255) NOT NULL UNIQUE, " +
                                        "polecenie VARCHAR(255) NOT NULL) AS SELECT * FROM CSVREAD('" + nazwa_pliku_csv + "')");
                                new SendStatement("DROP TABLE Test");

                                //usuwanie obecnej tabeli i utworzenie jej na nowo na podstawie pliku CSV
                                new SendStatement("DROP TABLE Commands");
                                new SendStatement("CREATE TABLE Commands (" +
                                        "id INT IDENTITY NOT NULL PRIMARY KEY, " +
                                        "wyraz VARCHAR(255) NOT NULL UNIQUE, " +
                                        "polecenie VARCHAR(255) NOT NULL) AS SELECT * FROM CSVREAD('" + nazwa_pliku_csv + "')");

                                JOptionPane.showMessageDialog(VoiceControlPCServer, "Pomyślnie zaimportowano plik CSV\nNależy ponownie uruchomić serwer", "Import CSV", JOptionPane.INFORMATION_MESSAGE);
                                System.exit(0);

                            } catch (Exception ex) {
                                JOptionPane.showMessageDialog(VoiceControlPCServer, "Błąd przy importowaniu pliku CSV\nSprawdź czy masz dostęp do ścieżki z plikiem oraz czy wybrano prawidłowy plik CSV\n", "Import CSV", JOptionPane.ERROR_MESSAGE);
                                ex.printStackTrace();
                            }
                        }
                    }
                }
        );

        button_new.addActionListener(
                e -> {
                    //Elementy okna
                    JTextField podany_wyraz = new JTextField(40);
                    podane_polecenie = new JTextField(40);
                    JButton choose_app = new JButton("Wybierz aplikację");
                    choose_app.addActionListener(this);

                    //ustawienie kursora na pole tekstowe wyrazu
                    podany_wyraz.addHierarchyListener( new RequestFocusListener() );

                    //tworzymy nowy JPanel a w nim niestandardowe okno dialogowe
                    JPanel PanelDialogu = new JPanel();
                    PanelDialogu.setLayout(new BoxLayout(PanelDialogu,BoxLayout.Y_AXIS));
                    PanelDialogu.add(new JLabel("Wyraz:"));
                    PanelDialogu.add(podany_wyraz);
                    PanelDialogu.add(Box.createVerticalStrut(15)); //odstęp
                    PanelDialogu.add(new JLabel("Polecenie:"));
                    PanelDialogu.add(podane_polecenie);
                    PanelDialogu.add(choose_app);

                    //tworzenie okna dialogowego
                    int temp = JOptionPane.showConfirmDialog(null, PanelDialogu,
                            "Nowe polecenie", JOptionPane.OK_CANCEL_OPTION, JOptionPane.INFORMATION_MESSAGE);
                    if (temp == JOptionPane.OK_OPTION) {
                        //sprawdzanie czy podano pusty wyraz/polecenie
                        if(StringUtils.isBlank(podany_wyraz.getText())){
                            JOptionPane.showMessageDialog(PanelDialogu, "Nie podano wyrazu!", "Błąd", JOptionPane.ERROR_MESSAGE);
                        }
                        else if(StringUtils.isBlank(podane_polecenie.getText())){
                            JOptionPane.showMessageDialog(PanelDialogu, "Nie podano polecenia!", "Błąd", JOptionPane.ERROR_MESSAGE);
                        }
                        else {
                            //tworzenie nowego wpisu w bazie danych i tablicy
                            String wyraz = podany_wyraz.getText();
                            String polecenie = podane_polecenie.getText();

                            //sprawdzamy czy podano wyraz specjalny
                            if (wyraz.equals("zrzut") || wyraz.equals("kamera")) {
                                JOptionPane.showMessageDialog(PanelDialogu, "Podany wyraz: " + wyraz + " jest zarezerwowany!", "Błąd", JOptionPane.ERROR_MESSAGE);
                            }
                            else {
                                DodajPolecenie(wyraz, polecenie, PanelDialogu);
                            }
                        }
                    }
                }
        );

        button_edit.addActionListener(
                e -> {
                    //sprawdzamy czy wybrano polecenie w tabeli
                    int wiersz = commandsJTable.getSelectedRow();
                    if(wiersz == -1){
                        JOptionPane.showMessageDialog(VoiceControlPCServer, "Nie wybrano polecenia!", "Błąd", JOptionPane.ERROR_MESSAGE);
                    }
                    else {
                        //pobieranie istniejących wartości, w tym numeru ID
                        String obecny_wyraz = (String) tabela.getValueAt(commandsJTable.convertRowIndexToModel(wiersz), 0);
                        String obecne_polecenie = (String) tabela.getValueAt(commandsJTable.convertRowIndexToModel(wiersz), 1);
                        Query q1 = em.createQuery("SELECT id FROM Commands WHERE wyraz='" +
                                obecny_wyraz + "' AND polecenie='" +
                                obecne_polecenie + "'");
                        Number[] obecny_id = {(Number) q1.getSingleResult()};

                        //elementy okna
                        JTextField podany_wyraz = new JTextField(40);
                        podane_polecenie = new JTextField(40);
                        JButton choose_app = new JButton("Wybierz aplikację");
                        choose_app.addActionListener(this);

                        //ustanowienie kursowa na pole tekstowe wyrazu
                        podany_wyraz.addHierarchyListener( new RequestFocusListener() );

                        podany_wyraz.setText(obecny_wyraz);
                        podane_polecenie.setText(obecne_polecenie);

                        //tworzymy nowy JPanel a w nim niestandardowe okno dialogowe
                        JPanel PanelDialogu = new JPanel();
                        PanelDialogu.setLayout(new BoxLayout(PanelDialogu, BoxLayout.Y_AXIS));
                        PanelDialogu.add(new JLabel("Wyraz:"));
                        PanelDialogu.add(podany_wyraz);
                        PanelDialogu.add(Box.createVerticalStrut(15)); //odstęp
                        PanelDialogu.add(new JLabel("Polecenie:"));
                        PanelDialogu.add(podane_polecenie);
                        PanelDialogu.add(choose_app);

                        //tworzenie okna dialogowego
                        int temp = JOptionPane.showConfirmDialog(null, PanelDialogu,
                                "Edytuj polecenie", JOptionPane.OK_CANCEL_OPTION, JOptionPane.INFORMATION_MESSAGE);
                        if (temp == JOptionPane.OK_OPTION) {
                            //sprawdzanie czy podano pusty wyraz/polecenie
                            if (StringUtils.isBlank(podany_wyraz.getText())) {
                                JOptionPane.showMessageDialog(PanelDialogu, "Nie podano wyrazu!", "Błąd", JOptionPane.ERROR_MESSAGE);
                            }
                            else if (StringUtils.isBlank(podane_polecenie.getText())) {
                                JOptionPane.showMessageDialog(PanelDialogu, "Nie podano polecenia!", "Błąd", JOptionPane.ERROR_MESSAGE);
                            }
                            else {
                                //uaktualnianie wpisu w bazie danych i tablicy
                                String wyraz = podany_wyraz.getText();
                                String polecenie = podane_polecenie.getText();

                                //sprawdzamy czy podano wyraz specjalny
                                if (wyraz.equals("zrzut") || wyraz.equals("kamera")) {
                                    JOptionPane.showMessageDialog(PanelDialogu, "Podany wyraz: " + wyraz + " jest zarezerwowany!", "Błąd", JOptionPane.ERROR_MESSAGE);
                                }

                                else {
                                    //sprawdzamy czy wpisano dwa razy ten sam wyraz
                                    Query[] que = {em.createQuery("SELECT COUNT(*) from Commands where wyraz='" + wyraz + "'")};
                                    Number[] wyraz_exists = {(Number) que[0].getSingleResult()};

                                    //sprawdzamy czy wyraz się nie zmienił, lub czy podano istniejący wyraz
                                    if (obecny_wyraz.equals(wyraz) || wyraz_exists[0].intValue() != 1) {
                                        em.getTransaction().begin();
                                        Commands command = em.find(Commands.class, obecny_id[0].intValue());
                                        command.setWyraz(wyraz);
                                        command.setPolecenie(polecenie);
                                        em.getTransaction().commit();

                                        tabela.setValueAt(wyraz, commandsJTable.convertRowIndexToModel(wiersz), 0);
                                        tabela.setValueAt(polecenie, commandsJTable.convertRowIndexToModel(wiersz), 1);
                                    }
                                    else {
                                        JOptionPane.showMessageDialog(PanelDialogu, "Podany wyraz: " + wyraz + " już istnieje!", "Błąd", JOptionPane.ERROR_MESSAGE);
                                    }
                                }
                            }
                        }
                    }
                }
        );

        button_delete.addActionListener(
                e -> {
                    //sprawdzamy czy wybrano w tabeli polecenie
                    int wiersz = commandsJTable.getSelectedRow();
                    if(wiersz == -1){
                        JOptionPane.showMessageDialog(VoiceControlPCServer, "Nie wybrano polecenia!", "Błąd", JOptionPane.ERROR_MESSAGE);
                    }
                    else {
                        //pobieranie istniejących wartości, w tym numer ID
                        String obecny_wyraz = (String) tabela.getValueAt(commandsJTable.convertRowIndexToModel(wiersz), 0);
                        String obecne_polecenie = (String) tabela.getValueAt(commandsJTable.convertRowIndexToModel(wiersz), 1);
                        Query q1 = em.createQuery("SELECT id FROM Commands WHERE wyraz='" +
                                obecny_wyraz + "' AND polecenie='" +
                                obecne_polecenie + "'");
                        final Number[] obecny_id = {(Number) q1.getSingleResult()};

                        //tworzenie okna dialogowego
                        int temp = JOptionPane.showConfirmDialog(null, "Czy napewno chcesz usunąć wybrany wpis?",
                                "Usuń polecenie", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
                        if (temp == JOptionPane.OK_OPTION) {
                            //usuwanie wpisu z bazy danych i tabeli
                            em.getTransaction().begin();
                            Commands command = em.find(Commands.class, obecny_id[0].intValue());
                            em.remove(command);
                            em.getTransaction().commit();

                            commandsList.remove(command);
                            tabela.remove();

                            q[0] = em.createQuery("SELECT COUNT(*) FROM Commands");
                            ilosc[0] = (Number) q[0].getSingleResult();
                            amountTXT.setText("Ilość poleceń: " + ilosc[0].toString());
                            }
                        }
                }
        );

        button_example.addActionListener(
                e -> {
                    //Sprawdzamy system operacyjny
                    boolean isSystemWindows = System.getProperty("os.name").toLowerCase().startsWith("windows");

                    //tworzymy przyciski
                    JButton shutdown = new JButton("Wyłącz komputer (wyłącz)");
                    JButton reset = new JButton("Uruchom ponownie komputer (reset)");

                    //tworzenie widoku przycisków
                    JPanel PanelDialogu = new JPanel();
                    PanelDialogu.setLayout(new BoxLayout(PanelDialogu,BoxLayout.Y_AXIS));
                    PanelDialogu.add(shutdown);
                    PanelDialogu.add(reset);

                    //obsługa przycisków w zależności od systemu
                    if(isSystemWindows) {
                        shutdown.addActionListener(e1 -> DodajPolecenie("wyłącz", "shutdown /s /t 0", PanelDialogu));
                        reset.addActionListener(e1 -> DodajPolecenie("reset", "shutdown /r /t 0", PanelDialogu));
                    }
                    else{
                        shutdown.addActionListener(e1 -> DodajPolecenie("wyłącz", "systemctl poweroff", PanelDialogu));
                        reset.addActionListener(e1 -> DodajPolecenie("reset", "systemctl reboot", PanelDialogu));
                    }

                    //pokazywanie dialogu z przyciskami
                    JOptionPane.showOptionDialog(null, PanelDialogu,"Przykładowe polecenia", JOptionPane.DEFAULT_OPTION,JOptionPane.INFORMATION_MESSAGE, null, new Object[]{}, null);
                }
        );

        //Wątek ciągłego czekania na klienta i ustanowienie z nim połączenia
        SwingWorker SocketWorker = new SwingWorker<String, Void>() {
            @Override
            @SuppressWarnings("InfiniteLoopStatement")
            protected String doInBackground() {

                ServerSocket serverSocket = null;

                try {
                    serverSocket = new ServerSocket(8163);
                }
                catch (IOException e) {
                    e.printStackTrace();
                }

                while (true) {
                    Socket socket = null;

                    //połączenie z klientem i utworzenie dla niego struktur odczytywania i zapisywania
                    try {
                        assert serverSocket != null;
                        socket = serverSocket.accept();
                        BufferedReader inputStream = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                        DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                        new Thread(new ClientThread(socket, inputStream, dos)).start();
                    }
                    catch (IOException e) {
                        try {
                            assert socket != null;
                            socket.close();
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }
                        e.printStackTrace();
                    }
                }
            }
        };

        //otwieramy socket i czekamy na klientów
        SocketWorker.execute();

        //uruchamianie ciągłego w tle ozgłaszania obecności serwera do potencjalnych klientów poprzez UDPBroadcast
        new Thread(new UDPBroadcast()).start();
    }

    //wątek połączenia z klientem
    class ClientThread implements Runnable{

        final BufferedReader inputStream;
        final Socket socket;
        final DataOutputStream dos;

        ClientThread(Socket socket, BufferedReader inputStream, DataOutputStream dos) {
            this.socket = socket;
            this.inputStream = inputStream;
            this.dos = dos;
        }

        @Override
        public void run()
        {
            //pobranie nazwy telefonu - pierwsza wiadomość odebrana od klienta
            try {
                phonenameTXT.setText(inputStream.readLine());
                statusTXT.setForeground(new Color(0, 153, 0));
                statusTXT.setText("Pomyślnie połączono z telefonem");
            } catch (IOException e) {
                e.printStackTrace();
            }

            //ciągłe oczekiwanie na nowe polecenia od klienta
            while(true){
                try{
                    String odpowiedz = inputStream.readLine(); // odpowiedź od klienta

                    //gdy nadeszła wiadomość stopu, rozłączamy się z klientem
                    if (StringUtils.isBlank(odpowiedz)) {
                        statusTXT.setForeground(Color.red);
                        statusTXT.setText("Utracono połączenie z telefonem");
                        phonenameTXT.setText("");
                        this.socket.close();
                        break;
                    }

                    else{
                        //zamiana na małą literę, ponieważ Google Voice Search czasami przesyła wyraz zaczynając od wielkiej litery
                        odpowiedz = odpowiedz.toLowerCase();

                        //funkcja wykonywania zrzutu ekranu i przesyłania go do telefonu gdy podano wyraz specjalny
                        if(odpowiedz.equals("zrzut")){
                            try {
                                //utworzenie zrzutu ekranu
                                BufferedImage screenshot = new Robot().createScreenCapture(new Rectangle(Toolkit.getDefaultToolkit().getScreenSize()));
                                new SendImage(screenshot, dos);
                                statusTXT.setForeground(new Color(0, 153, 0));
                                statusTXT.setText("Pomyślnie przesłano zrzut ekranu");
                            }
                            catch (AWTException ex) {
                                ex.printStackTrace();
                            }
                        }

                        //funkcja wykonywania zrzutu obrazu kamery i przesyłania go do telefonu
                        else if(odpowiedz.equals("kamera")){
                            //jeżeli nie znaleziono kamery, wyślij stosowną odpowiedź klientowi
                            if(no_webcam){
                                dos.writeInt(1);
                                dos.flush();
                                statusTXT.setForeground(Color.red);
                                statusTXT.setText("Brak podłączonej kamery internetowej");
                            }
                            else {
                                //tworzenie zrzutu z kamery
                                BufferedImage kamera = kamera_internetowa.getImage();
                                new SendImage(kamera, dos);
                                statusTXT.setForeground(new Color(0, 153, 0));
                                statusTXT.setText("Pomyślnie przesłano obraz kamery internetowej");
                            }
                        }

                        else {
                            //przechwycenie polecenia zgodniego z wyrazem z telefonu
                            Query query = em.createQuery("SELECT polecenie from Commands where wyraz='" + odpowiedz + "'");
                            List temp_list = query.getResultList();
                            //zamiana List na String - Apache Commons Lang
                            String polecenie = StringUtils.join(temp_list, "");

                            //Jeżeli nie znaleziono połączenia, string jest pusty
                            if(polecenie.equals("")){
                                statusTXT.setForeground(Color.red);
                                statusTXT.setText("Nie znaleziono wyrazu '" + odpowiedz + "' w bazie poleceń");
                            }
                            //wykonywanie polecenia
                            else {
                                CommandLine wiersz_polecen = CommandLine.parse(polecenie);
                                DefaultExecuteResultHandler handler = new DefaultExecuteResultHandler();
                                DefaultExecutor executor = new DefaultExecutor();
                                executor.execute(wiersz_polecen, handler);

                                statusTXT.setForeground(new Color(0, 153, 0));
                                statusTXT.setText("Pomyślnie zrealizowano polecenie: " + odpowiedz);
                            }
                        }
                    }
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
            try
            {
                this.inputStream.close();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    //funkcja przycisku "Wybierz aplikację"
    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        JFileChooser filechooser = new JFileChooser();
        filechooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        int returnVal = filechooser.showOpenDialog(VoiceControlPCServer);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            //pobieramy nazwę wybranej aplikacji
            podane_polecenie.setText(filechooser.getSelectedFile().getAbsolutePath());
        }
    }

    //funkcja wyszukiwania
    private void Filtr() {
        RowFilter<TableModel, Object> filtr;
        try {
            filtr = RowFilter.regexFilter(searchTXT.getText(), 0, 1);
        }
        catch (java.util.regex.PatternSyntaxException e) {
            return;
        }
        sorter.setRowFilter(filtr);
    }

    //funkcja minimalizacji do paska zadań
    private static void frameMinimalizacja(WindowEvent e){
        if ((e.getNewState() & Frame.ICONIFIED) == Frame.ICONIFIED){
            frame.setVisible(false);
        }
    }

    private void DodajPolecenie(String wyraz, String polecenie, JPanel PanelDialogu){
        //sprawdzamy czy wpisano dwa razy ten sam wyraz
        Query[] que = {em.createQuery("SELECT COUNT(*) from Commands where wyraz='" + wyraz + "'")};
        Number[] wyraz_exists = {(Number) que[0].getSingleResult()};

        if (wyraz_exists[0].intValue() != 1) {
            em.getTransaction().begin();
            Commands command = new Commands();
            command.setWyraz(wyraz);
            command.setPolecenie(polecenie);
            em.persist(command);
            em.getTransaction().commit();

            commandsList.add(command);
            tabela.add();

            Query[] q = {em.createQuery("SELECT COUNT(*) FROM Commands")};
            Number[] ilosc = {(Number) q[0].getSingleResult()};
            amountTXT.setText("Ilość polecen: " + ilosc[0].toString());
        }
        else {
            JOptionPane.showMessageDialog(PanelDialogu, "Podany wyraz: " + wyraz + " już istnieje!", "Błąd", JOptionPane.ERROR_MESSAGE);
        }
    }

    //właściwości interfejsu graficznego
    private static void createAndShowGUI() {
        frame = new JFrame("VoiceControlPC Server");
        frame.setResizable(false); //nie zezwalaj na zmianę rozmiaru
        frame.setContentPane(new VoiceControlPCServer().VoiceControlPCServer);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        //nasłuchiwanie przycisku minimalizacji - minimalizacja do paska zadań
        frame.addWindowStateListener(com.mskrzynski.voicecontrolpcserver.VoiceControlPCServer::frameMinimalizacja);
        frame.pack();
        frame.setVisible(true);

        //tworzenie ikony na pasku zadań - jeżeli to możliwe
        if (SystemTray.isSupported()) {
            try {
                final SystemTray tray = SystemTray.getSystemTray();
                //ustawienie ikony
                InputStream inputStreamIcon = ClassLoader.getSystemClassLoader().getResourceAsStream("icon.gif");
                assert inputStreamIcon != null;
                BufferedImage ikona = ImageIO.read(inputStreamIcon);
                int trayIconWidth = new TrayIcon(ikona).getSize().width;
                //gładka ikona w pasku zadań
                TrayIcon trayIkona = new TrayIcon(ikona.getScaledInstance(trayIconWidth, -1, Image.SCALE_SMOOTH), "VoiceControlPC Server");

                tray.add(trayIkona);

                //reagowanie na kliknięcie na ikonę na pasku zadań
                trayIkona.addMouseListener(new MouseListener() {
                    @Override
                    public void mouseClicked( MouseEvent mouseEvent ) {
                        frame.setVisible(true);
                        frame.setState (Frame.NORMAL);
                    }

                    @Override
                    public void mousePressed(MouseEvent mouseEvent) {}

                    @Override
                    public void mouseReleased(MouseEvent mouseEvent) {}

                    @Override
                    public void mouseEntered(MouseEvent mouseEvent) {}

                    @Override
                    public void mouseExited(MouseEvent mouseEvent) {}
                });
            }
            catch (AWTException | IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        //utworzenie interfejsu graficznego
        javax.swing.SwingUtilities.invokeLater(com.mskrzynski.voicecontrolpcserver.VoiceControlPCServer::createAndShowGUI);

        //połączenie z bazą danych i utworzenie pliku bazy danych przy pierwszym uruchomieniu
        Connection polaczenie = null;
        try {
            //sprawdzamy czy plik bazy danych baza już istnieje
            polaczenie = DriverManager.getConnection("jdbc:h2:~/VoiceControlPC_Database/database;IFEXISTS=TRUE");
        }
        catch (final Exception e) {
            //tworzymy tabelę przy pierwszym uruchomieniu
                new SendStatement("CREATE TABLE Commands (" +
                        "id INT IDENTITY NOT NULL PRIMARY KEY, " +
                        "wyraz VARCHAR(255) NOT NULL UNIQUE, " +
                        "polecenie VARCHAR(255) NOT NULL)");
        }
        finally{
            try {
                assert polaczenie != null;
                polaczenie.close();
            }
            catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}