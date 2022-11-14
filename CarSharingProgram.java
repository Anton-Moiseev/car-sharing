package carsharing;

import carsharing.utils.PrintUtils;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

class CarSharingProgram {
    private Connection connection;
    private final Scanner scanner = new Scanner(System.in);

    void run(String[] commandLineArguments) throws Exception {
        makeDbConnection(commandLineArguments);
        createTables();
        runMainMenu();
        connection.close();
    }

    private void makeDbConnection(String[] commandLineArguments) throws SQLException,
                                                                        ClassNotFoundException {
        List<String> argsArrayList = new ArrayList<>(List.of(commandLineArguments));
        String dbName;
        if (argsArrayList.contains("-databaseFileName")) {
            dbName = argsArrayList.get(1);
        } else {
            dbName = "anything";
        }
        Class.forName("org.h2.Driver");
        String url = "jdbc:h2:file:../task/src/carsharing/db/" + dbName;
        connection = DriverManager.getConnection(url);
        connection.setAutoCommit(true);
    }

    private void createTables() throws SQLException {
        String query = "CREATE TABLE IF NOT EXISTS COMPANY(" +
                "ID INT PRIMARY KEY AUTO_INCREMENT," +
                " NAME VARCHAR(30) NOT NULL UNIQUE);";
        Statement statement = connection.createStatement();
        statement.executeUpdate(query);
        query = "CREATE TABLE IF NOT EXISTS CAR(" +
                "ID INT PRIMARY KEY AUTO_INCREMENT," +
                " NAME VARCHAR(30) NOT NULL UNIQUE," +
                " COMPANY_ID INT NOT NULL," +
                " CONSTRAINT fk_company_id FOREIGN KEY (COMPANY_ID)" +
                " REFERENCES COMPANY(ID));";
        statement.executeUpdate(query);
        query = "CREATE TABLE IF NOT EXISTS CUSTOMER(" +
                "ID INT PRIMARY KEY AUTO_INCREMENT," +
                " NAME VARCHAR(30) NOT NULL UNIQUE," +
                " RENTED_CAR_ID INT DEFAULT NULL," +
                " CONSTRAINT fk_car_id FOREIGN KEY (RENTED_CAR_ID)" +
                " REFERENCES CAR(ID));";
        statement.executeUpdate(query);
    }

    private void runMainMenu() throws SQLException {
        PrintUtils.mainMenu();
        int pick = Integer.parseInt(scanner.nextLine().trim());
        if (pick == 0) {
            return;
        }
        switch (pick) {
            case 1:
                runManagerMenu();
                break;
            case 2:
                runClientsMenu();
                break;
            case 3:
                createClient();
                runMainMenu();
        }
    }

    private void runManagerMenu() throws SQLException {
        PrintUtils.managerMenu();
        int pick = Integer.parseInt(scanner.nextLine().trim());
        if (pick == 0) {
            runMainMenu();
            return;
        }
        switch (pick) {
            case 1:
                runCompaniesMenu();
                break;
            case 2:
                createCompany();
                runManagerMenu();
        }
    }

    private void runClientsMenu() throws SQLException {
        String query = "SELECT * FROM CUSTOMER;";
        Statement statement =
                connection.createStatement(
                        ResultSet.TYPE_SCROLL_INSENSITIVE,
                        ResultSet.CONCUR_READ_ONLY);
        ResultSet clientsResultSet = statement.executeQuery(query);
        PrintUtils.clientsList(clientsResultSet);
        clientsResultSet.last();
        int size = clientsResultSet.getRow();
        if (size == 0) {
            runMainMenu();
            return;
        }
        int pick = Integer.parseInt(scanner.nextLine().trim());
        if (pick == 0) {
            runMainMenu();
            return;
        }
        String name = getPickedName(clientsResultSet, pick);
        query = "SELECT * FROM CUSTOMER" +
                " WHERE NAME = " + "'" + name + "'" + ";";
        ResultSet clientResultSet = statement.executeQuery(query);
        runSingleClientMenu(clientResultSet);
    }

    private void runSingleClientMenu(ResultSet clientResultSet) throws SQLException {
        clientResultSet = updateClientResultSet(clientResultSet);
        PrintUtils.singleClientMenu();
        int pick = Integer.parseInt(scanner.nextLine().trim());
        if (pick == 0) {
            runMainMenu();
            return;
        }
        switch (pick) {
            case 1:
                rentCar(clientResultSet);
                runSingleClientMenu(clientResultSet);
                break;
            case 2:
                returnCar(clientResultSet);
                runSingleClientMenu(clientResultSet);
                break;
            case 3:
                myRentedCar(clientResultSet);
                runSingleClientMenu(clientResultSet);
        }
    }

    private void rentCar(ResultSet clientResultSet) throws SQLException {
        clientResultSet.next();
        int rentedCarId = clientResultSet.getInt("RENTED_CAR_ID");
        clientResultSet.beforeFirst();
        if (rentedCarId != 0) {
            PrintUtils.alreadyRented();
            runSingleClientMenu(clientResultSet);
            return;
        }
        String query = "SELECT * FROM COMPANY;";
        Statement statement =
                connection.createStatement(
                        ResultSet.TYPE_SCROLL_INSENSITIVE,
                        ResultSet.CONCUR_READ_ONLY);
        ResultSet resultSet = statement.executeQuery(query);
        PrintUtils.chooseCompany();
        PrintUtils.companyList(resultSet);
        resultSet.last();
        int size = resultSet.getRow();
        if (size == 0) {
            runSingleClientMenu(clientResultSet);
            return;
        }
        int pick = Integer.parseInt(scanner.nextLine().trim());
        if (pick == 0) {
            runSingleClientMenu(clientResultSet);
            return;
        }
        int companyId = getPickedId(resultSet, pick);
        query = "SELECT * FROM CAR" +
                " WHERE COMPANY_ID = " + companyId + " AND" +
                " ID NOT IN (" +
                "SELECT RENTED_CAR_ID" +
                " FROM CUSTOMER" +
                " WHERE RENTED_CAR_ID IS NOT NULL);";
        ResultSet carsResultSet = statement.executeQuery(query);
        PrintUtils.carList(carsResultSet);
        carsResultSet.last();
        size = carsResultSet.getRow();
        if (size == 0) {
            runSingleClientMenu(clientResultSet);
            return;
        }
        pick = Integer.parseInt(scanner.nextLine().trim());
        if (pick == 0) {
            runSingleClientMenu(clientResultSet);
            return;
        }
        clientResultSet.next();
        int customerId = clientResultSet.getInt("ID");
        clientResultSet.beforeFirst();
        int carId = getPickedId(carsResultSet, pick);
        String carName = getPickedName(carsResultSet, pick);
        query = "UPDATE CUSTOMER" +
                " SET RENTED_CAR_ID = " + carId +
                " WHERE ID = " + customerId + ";";
        statement.executeUpdate(query);
        PrintUtils.youRented(carName);
    }

    private void returnCar(ResultSet clientResultSet) throws SQLException {
        clientResultSet.next();
        int rentedCarId = clientResultSet.getInt("RENTED_CAR_ID");
        clientResultSet.beforeFirst();
        if (rentedCarId == 0) {
            PrintUtils.didntRent();
            return;
        }
        String query = "UPDATE CUSTOMER" +
                        " SET RENTED_CAR_ID = NULL" +
                        " WHERE RENTED_CAR_ID = " + rentedCarId + ";";
        Statement statement =
                connection.createStatement(
                        ResultSet.TYPE_SCROLL_INSENSITIVE,
                        ResultSet.CONCUR_READ_ONLY);
        statement.executeUpdate(query);
        PrintUtils.returnedCar();
    }

    private void myRentedCar(ResultSet clientResultSet) throws SQLException {
        clientResultSet.next();
        int rentedCarId = clientResultSet.getInt("RENTED_CAR_ID");
        clientResultSet.beforeFirst();
        if (rentedCarId == 0) {
            PrintUtils.didntRent();
            return;
        }
        String query = "SELECT * FROM CAR" +
                        " WHERE ID = " + rentedCarId + ";";
        Statement statement =
                connection.createStatement(
                        ResultSet.TYPE_SCROLL_INSENSITIVE,
                        ResultSet.CONCUR_READ_ONLY);
        ResultSet carResultSet = statement.executeQuery(query);
        carResultSet.next();
        String carName = carResultSet.getString("NAME");
        int companyID = carResultSet.getInt("COMPANY_ID");
        carResultSet.beforeFirst();
        query = "SELECT NAME FROM COMPANY" +
                " WHERE ID = " + companyID + ";";
        ResultSet companyResultSet = statement.executeQuery(query);
        companyResultSet.next();
        String companyName = companyResultSet.getString("NAME");
        companyResultSet.beforeFirst();
        PrintUtils.carYouRented(carName, companyName);
    }

    private void createClient() throws SQLException {
        PrintUtils.enterClientName();
        String clientName = scanner.nextLine().trim();
        String query = "INSERT INTO CUSTOMER (NAME)" +
                "VALUES ('" + clientName + "');";
        Statement statement = connection.createStatement();
        statement.executeUpdate(query);
        PrintUtils.clientAdded();
    }

    private void createCompany() throws SQLException {
        PrintUtils.enterCompanyName();
        String companyName = scanner.nextLine().trim();
        String query = "INSERT INTO COMPANY (NAME)" +
                "VALUES ('" + companyName + "');";
        Statement statement = connection.createStatement();
        statement.executeUpdate(query);
        PrintUtils.companyCreated();
    }

    private void runCompaniesMenu() throws SQLException {
        String query = "SELECT * FROM COMPANY;";
        Statement statement =
                connection.createStatement(
                        ResultSet.TYPE_SCROLL_INSENSITIVE,
                        ResultSet.CONCUR_READ_ONLY);
        ResultSet resultSet = statement.executeQuery(query);
        PrintUtils.companyList(resultSet);
        resultSet.last();
        int size = resultSet.getRow();
        if (size == 0) {
            runManagerMenu();
            return;
        }
        int pick = Integer.parseInt(scanner.nextLine().trim());
        if (pick == 0) {
            runManagerMenu();
            return;
        }
        int companyId = getPickedId(resultSet, pick);
        query = "SELECT * FROM COMPANY" +
                " WHERE ID = " + companyId + ";";
        ResultSet companyResultSet = statement.executeQuery(query);
        runSingleCompanyMenu(companyResultSet);
    }

    private void runSingleCompanyMenu(ResultSet companyResultSet) throws SQLException {
        companyResultSet.next();
        String name = companyResultSet.getString("NAME");
        int companyId = companyResultSet.getInt("ID");
        companyResultSet.beforeFirst();
        PrintUtils.singleCompanyMenu(name);
        int pick = Integer.parseInt(scanner.nextLine().trim());
        if (pick == 0) {
            runManagerMenu();
            return;
        }
        String query = "SELECT * FROM CAR" +
                        " WHERE COMPANY_ID = " + companyId + ";";
        Statement statement =
                connection.createStatement(
                        ResultSet.TYPE_SCROLL_INSENSITIVE,
                        ResultSet.CONCUR_READ_ONLY);
        ResultSet resultSet = statement.executeQuery(query);
        switch (pick) {
            case 1:
                PrintUtils.carList(resultSet);
                runSingleCompanyMenu(companyResultSet);
                break;
            case 2:
                createCar(companyId);
                runSingleCompanyMenu(companyResultSet);
        }
    }

    private void createCar(int id) throws SQLException {
        PrintUtils.enterCarName();
        String carName = scanner.nextLine().trim();
        String query = "INSERT INTO CAR (NAME, COMPANY_ID)" +
                "VALUES ('" + carName + "', " + id + ");";
        Statement statement = connection.createStatement();
        statement.executeUpdate(query);
        PrintUtils.carCreated();
    }

    private String getPickedName(ResultSet resultSet, int pick) throws SQLException {
        resultSet.beforeFirst();
        int iterationNumber = 1;
        String name = "";
        while (resultSet.next()) {
            if (pick == iterationNumber) {
                name = resultSet.getString("NAME");
            }
            ++iterationNumber;
        }
        return name;
    }

    private int getPickedId(ResultSet resultSet, int pick) throws SQLException {
        resultSet.beforeFirst();
        int iterationNumber = 1;
        int id = 0;
        while (resultSet.next()) {
            if (pick == iterationNumber) {
                id = resultSet.getInt("ID");
            }
            ++iterationNumber;
        }
        return id;
    }

    private ResultSet updateClientResultSet(ResultSet clientResultSet) throws SQLException {
        clientResultSet.next();
        int clientId = clientResultSet.getInt("ID");
        clientResultSet.beforeFirst();
        String query = "SELECT * FROM CUSTOMER" +
                " WHERE ID = " + clientId + ";";
        Statement statement =
                connection.createStatement(
                        ResultSet.TYPE_SCROLL_INSENSITIVE,
                        ResultSet.CONCUR_READ_ONLY);
        clientResultSet = statement.executeQuery(query);
        return clientResultSet;
    }

    private ResultSet updateCarResultSet(ResultSet carResultSet) throws SQLException {
        carResultSet.next();
        int carId = carResultSet.getInt("ID");
        carResultSet.beforeFirst();
        String query = "SELECT * FROM CAR" +
                " WHERE ID = " + carId + ";";
        Statement statement =
                connection.createStatement(
                        ResultSet.TYPE_SCROLL_INSENSITIVE,
                        ResultSet.CONCUR_READ_ONLY);
        carResultSet = statement.executeQuery(query);
        return carResultSet;
    }
}
