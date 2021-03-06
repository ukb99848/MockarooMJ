package be.vdab.dao;

import be.vdab.model.BitcoinAccount;
import be.vdab.model.NotEnoughBalanceException;

import java.sql.*;
import java.util.ArrayList;

public class BitAccDaoImpl implements BitAccDao{
    private String url;
    private String user;
    private String password;

    public BitAccDaoImpl(String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
    }

    @Override
    public BitcoinAccount getBitAccById(int id) throws SQLException {
        String query = "SELECT * FROM bitcnacc WHERE id=?";
        BitcoinAccount bitcoinAccount = new BitcoinAccount();
        try (
                Connection con = getConnection();
                PreparedStatement stmt = con.prepareStatement(query)
        ) {
            stmt.setInt(1, id);
            try (
                    ResultSet rs = stmt.executeQuery()
            ) {
                if (rs.next()) {
                    bitcoinAccount.setId(rs.getInt("id"));
                    bitcoinAccount.setBitcoinAddress(rs.getString("bitcoin_address"));
                    bitcoinAccount.setSaldo(rs.getDouble("saldo"));
                    bitcoinAccount.setCreditCardType(rs.getString("credit_card_type"));
                    bitcoinAccount.setOwnerId(rs.getInt("owner_id"));
                }
            }
        } catch (SQLException throwables) {
            throw new SQLException(throwables);
        }
        return bitcoinAccount;
    }

    @Override
    public void createBitAcc(BitcoinAccount nwBitAcc) throws SQLException {

    }

    @Override
    public void updateBitAcc(BitcoinAccount corBitAcc) throws SQLException {
        String update = "UPDATE bitcnacc SET bitcoin_address =?, saldo =?, credit_card_type=?" +
                ", owner_id=? WHERE id=?";
        try (
                Connection con = getConnection();
                PreparedStatement stmt = con.prepareStatement(update)
        ) {
            stmt.setString(1, corBitAcc.getBitcoinAddress());
            stmt.setDouble(2, corBitAcc.getSaldo());
            stmt.setString(3, corBitAcc.getCreditCardType());
            stmt.setInt(4, corBitAcc.getOwnerId());
            stmt.setInt(5, corBitAcc.getId());
            stmt.executeUpdate();
        } catch (SQLException throwables) {
            throw new SQLException(throwables);
        }
    }

    @Override
    public void deleteBitAcc(BitcoinAccount exBitAcc) throws SQLException {

    }

    @Override
    public double getBalance(BitcoinAccount curBitAcc) throws SQLException {
        String query = "SELECT * FROM bitcnacc WHERE id=?";
        double balance = 0;
        try (
                Connection con = getConnection();
                PreparedStatement stmt = con.prepareStatement(query)
        ) {
            stmt.setInt(1, curBitAcc.getId());
            try (
                    ResultSet rs = stmt.executeQuery()
            ) {
                BitcoinAccount bitcoinAccount;
                if (rs.next()) {
                    bitcoinAccount = new BitcoinAccount();
                    balance = rs.getDouble("saldo");
                }
            }
        } catch (SQLException throwables) {
            throw new SQLException(throwables);
        }
        return balance;
    }

    @Override
    public void pay(BitcoinAccount corBitAcc,double amount) throws NotEnoughBalanceException {
        try {
            //get balance inputaccount
            double inbalance = this.getBalance(corBitAcc);
            // is balance enough?
            if (inbalance>=amount){
                // withdraw amount
                corBitAcc.setSaldo(corBitAcc.getSaldo()-amount);
                // update DB
                this.updateBitAcc(corBitAcc);
            }else{
                throw new NotEnoughBalanceException("Balance is too low !");
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        catch (NotEnoughBalanceException ex){
            throw ex;
        }

    }

    @Override
    public void receivePayment(BitcoinAccount corBitAcc,double amount) {
        try {
            // deposit amount
            corBitAcc.setSaldo(corBitAcc.getSaldo()+amount);
            // update db
            this.updateBitAcc(corBitAcc);
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    @Override
    public void executePaymentTxn(BitcoinAccount payerAccount, BitcoinAccount receiverAccount, double amount)
            throws NotEnoughBalanceException {
        try {
            //get balance inputaccount
            double inbalance = this.getBalance(payerAccount);
            // is balance enough?
            if (inbalance>=amount){
                System.out.println("Balance paying account is sufficient : " + inbalance);
                // withdraw amount
                payerAccount.setSaldo(payerAccount.getSaldo()-amount);
                // deposit amount
                receiverAccount.setSaldo(receiverAccount.getSaldo()+amount);
                // update txn
                updateTxnBitAcc(payerAccount,receiverAccount);
            }else{
                throw new NotEnoughBalanceException("Balance payee is too low : " + inbalance + " !");
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
            System.out.println("Payment failed");
        }
        catch (NotEnoughBalanceException ex){
            throw ex;
        }

    }

    @Override
    public void updateTxnBitAcc(BitcoinAccount payerAccount, BitcoinAccount receiverAccount) {
        String update = "UPDATE bitcnacc SET bitcoin_address =?, saldo =?, credit_card_type=?" +
                ", owner_id=? WHERE id=?";
        try (Connection con = getConnection();){
            try (
                    PreparedStatement stmtPayer = con.prepareStatement(update);
                    PreparedStatement stmtReceiver = con.prepareStatement(update)
            ) {
                // Txn settings
                con.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
                con.setAutoCommit(false);
                // Fill in SQL stmts
                stmtPayer.setString(1, payerAccount.getBitcoinAddress());
                stmtPayer.setDouble(2, payerAccount.getSaldo());
                stmtPayer.setString(3, payerAccount.getCreditCardType());
                stmtPayer.setInt(4, payerAccount.getOwnerId());
                stmtPayer.setInt(5, payerAccount.getId());
                stmtPayer.executeUpdate();
                stmtReceiver.setString(1, receiverAccount.getBitcoinAddress());
                stmtReceiver.setDouble(2, receiverAccount.getSaldo());
                stmtReceiver.setString(3, receiverAccount.getCreditCardType());
                stmtReceiver.setInt(4, receiverAccount.getOwnerId());
                stmtReceiver.setInt(5, receiverAccount.getId());
                stmtReceiver.executeUpdate();
                con.commit();
                con.setAutoCommit(true);
            } catch (SQLException throwables) {
                con.rollback();
                System.out.println("rollback executed");
                throw new SQLException(throwables);
            }

        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }

    }

    @Override
    public ArrayList<BitcoinAccount> getBitAccByOwner(int id) throws SQLException {
        String query = "SELECT * FROM bitcnacc WHERE owner_id=?";
        try (
                Connection con = getConnection();
                PreparedStatement stmt = con.prepareStatement(query)
        ) {
            stmt.setInt(1, id);
            try (
                    ResultSet rs = stmt.executeQuery()
            ) {
                ArrayList<BitcoinAccount> bitAccList = new ArrayList<>();
                BitcoinAccount bitcoinAccount;
                while (rs.next()) {
                    bitcoinAccount = new BitcoinAccount();
                    bitcoinAccount.setId(rs.getInt("id"));
                    bitcoinAccount.setBitcoinAddress(rs.getString("bitcoin_address"));
                    bitcoinAccount.setSaldo(rs.getDouble("saldo"));
                    bitcoinAccount.setCreditCardType(rs.getString("credit_card_type"));
                    bitcoinAccount.setOwnerId(rs.getInt("owner_id"));
                    bitAccList.add(bitcoinAccount);
                }
                return bitAccList;
            }
        } catch (SQLException throwables) {
            throw new SQLException(throwables);
        }
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

}
