package com.dlis.payment.dao.jdbc;

import com.dlis.payment.domain.Payment;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentDaoImplTest {

    @Mock private DataSource dataSource;
    @Mock private Connection connection;
    @Mock private PreparedStatement ps;
    @Mock private ResultSet rs;

    @InjectMocks
    private PaymentDaoImpl dao;

    private Payment payment() {
        Payment p = new Payment();
        p.setApplicationId(10L);
        p.setCandidateId(1L);
        p.setPaymentDate(LocalDate.of(2025, 1, 2));
        p.setPaymentAmount(new BigDecimal("45.00"));
        p.setPaymentMethod("CC");
        p.setPaymentReference("REF1");
        p.setPaymentStatus("S");
        p.setLicenseType("L");
        p.setFeeType("IF");
        p.setReceiptNumber("RCP202501021");
        p.setProcessedBy("U");
        return p;
    }

    @Test
    void insert_bindsParametersAndSetsGeneratedKey() throws SQLException {
        Payment p = payment();
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString(), eq(Statement.RETURN_GENERATED_KEYS))).thenReturn(ps);
        when(ps.getGeneratedKeys()).thenReturn(rs);
        when(rs.next()).thenReturn(true);
        when(rs.getLong(1)).thenReturn(77L);

        dao.insert(p);

        verify(ps).setLong(1, 10L);
        verify(ps).setBigDecimal(4, new BigDecimal("45.00"));
        verify(ps).setString(6, "REF1");
        verify(ps).setString(11, "U");
        verify(ps).executeUpdate();
        assertEquals(77L, p.getPaymentId());
    }

    @Test
    void insert_sqlException_wrapped() throws SQLException {
        when(dataSource.getConnection()).thenThrow(new SQLException("db"));
        RuntimeException ex = assertThrows(RuntimeException.class, () -> dao.insert(payment()));
        assertTrue(ex.getMessage().contains("PaymentDao.insert"));
    }

    @Test
    void findByApplicationAndReference_notFound_returnsEmpty() throws SQLException {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(false);

        assertTrue(dao.findByApplicationAndReference(10L, "X").isEmpty());
        verify(ps).setLong(1, 10L);
        verify(ps).setString(2, "X");
    }

    @Test
    void findById_mapsRow() throws SQLException {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true);
        when(rs.getLong("PAYMENT_ID")).thenReturn(5L);
        when(rs.getLong("APPLICATION_ID")).thenReturn(10L);
        when(rs.getLong("CANDIDATE_ID")).thenReturn(1L);
        when(rs.getDate("PAYMENT_DATE")).thenReturn(Date.valueOf(LocalDate.of(2025, 1, 2)));
        when(rs.getBigDecimal("PAYMENT_AMOUNT")).thenReturn(new BigDecimal("45.00"));
        when(rs.getString(anyString())).thenReturn("X");
        when(rs.getString("PAYMENT_REFERENCE")).thenReturn("REF1");

        Optional<Payment> p = dao.findById(5L);

        assertTrue(p.isPresent());
        assertEquals(5L, p.get().getPaymentId());
        assertEquals("REF1", p.get().getPaymentReference());
    }
}
