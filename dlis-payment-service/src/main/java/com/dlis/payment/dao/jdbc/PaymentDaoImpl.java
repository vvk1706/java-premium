package com.dlis.payment.dao.jdbc;

import com.dlis.payment.dao.PaymentDao;
import com.dlis.payment.domain.Payment;

import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.sql.DataSource;
import java.sql.*;
import java.util.Optional;

/** JDBC DAO for PAYMENT (extracted from the inline JDBC that used to live in PaymentServiceEjb). */
@Stateless
public class PaymentDaoImpl implements PaymentDao {

    @Resource(lookup = "jdbc/dlisDS")
    private DataSource dataSource;

    private static final String INSERT_SQL =
        "INSERT INTO PAYMENT " +
        "(APPLICATION_ID,CANDIDATE_ID,PAYMENT_DATE,PAYMENT_AMOUNT," +
        " PAYMENT_METHOD,PAYMENT_REFERENCE,PAYMENT_STATUS,LICENSE_TYPE," +
        " FEE_TYPE,RECEIPT_NUMBER,PROCESSED_BY) " +
        "VALUES (?,?,?,?,?,?,?,?,?,?,?)";

    private static final String SELECT_COLUMNS =
        "SELECT PAYMENT_ID,APPLICATION_ID,CANDIDATE_ID,PAYMENT_DATE,PAYMENT_AMOUNT," +
        " PAYMENT_METHOD,PAYMENT_REFERENCE,PAYMENT_STATUS,LICENSE_TYPE,FEE_TYPE," +
        " RECEIPT_NUMBER,PROCESSED_BY,NOTES FROM PAYMENT ";

    @Override
    public void insert(Payment p) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, p.getApplicationId());
            ps.setLong(2, p.getCandidateId());
            ps.setDate(3, Date.valueOf(p.getPaymentDate()));
            ps.setBigDecimal(4, p.getPaymentAmount());
            ps.setString(5, p.getPaymentMethod());
            ps.setString(6, p.getPaymentReference());
            ps.setString(7, p.getPaymentStatus());
            ps.setString(8, p.getLicenseType());
            ps.setString(9, p.getFeeType());
            ps.setString(10, p.getReceiptNumber());
            ps.setString(11, p.getProcessedBy());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    p.setPaymentId(keys.getLong(1));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("PaymentDao.insert failed", e);
        }
    }

    @Override
    public Optional<Payment> findById(Long paymentId) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_COLUMNS + "WHERE PAYMENT_ID = ?")) {
            ps.setLong(1, paymentId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("PaymentDao.findById failed", e);
        }
    }

    @Override
    public Optional<Payment> findByApplicationAndReference(Long applicationId, String paymentReference) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_COLUMNS
                     + "WHERE APPLICATION_ID = ? AND PAYMENT_REFERENCE = ? FETCH FIRST 1 ROWS ONLY")) {
            ps.setLong(1, applicationId);
            ps.setString(2, paymentReference);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("PaymentDao.findByApplicationAndReference failed", e);
        }
    }

    private Payment mapRow(ResultSet rs) throws SQLException {
        Payment p = new Payment();
        p.setPaymentId(rs.getLong("PAYMENT_ID"));
        p.setApplicationId(rs.getLong("APPLICATION_ID"));
        p.setCandidateId(rs.getLong("CANDIDATE_ID"));
        p.setPaymentDate(rs.getDate("PAYMENT_DATE").toLocalDate());
        p.setPaymentAmount(rs.getBigDecimal("PAYMENT_AMOUNT"));
        p.setPaymentMethod(rs.getString("PAYMENT_METHOD"));
        p.setPaymentReference(rs.getString("PAYMENT_REFERENCE"));
        p.setPaymentStatus(rs.getString("PAYMENT_STATUS"));
        p.setLicenseType(rs.getString("LICENSE_TYPE"));
        p.setFeeType(rs.getString("FEE_TYPE"));
        p.setReceiptNumber(rs.getString("RECEIPT_NUMBER"));
        p.setProcessedBy(rs.getString("PROCESSED_BY"));
        p.setNotes(rs.getString("NOTES"));
        return p;
    }
}
