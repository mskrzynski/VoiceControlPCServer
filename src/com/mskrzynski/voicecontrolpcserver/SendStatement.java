package com.mskrzynski.voicecontrolpcserver;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

class SendStatement {
    SendStatement(String wyrazenie) {
        try {
            Connection connection = DriverManager.getConnection("jdbc:h2:~/VoiceControlPC_Database/database");
            Statement statement = connection.createStatement();
            statement.execute(wyrazenie);
            statement.close();
            connection.close();
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
