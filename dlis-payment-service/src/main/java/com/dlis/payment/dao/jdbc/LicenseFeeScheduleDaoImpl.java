package com.dlis.payment.dao.jdbc;

import com.dlis.payment.dao.LicenseFeeScheduleDao;
import com.dlis.payment.domain.*;

import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.sql.DataSource;
import java.sql.*;
import java.util.Optional;

@Stateless
class LicenseFeeScheduleDaoImpl implements LicenseFeeScheduleDao {

    @Resource(lookup = "jdbc/dlisDS")
    private DataSource dataSource;

    private static final String SELECT_ACTIVE_FEE =
        "SELECT FEE_SCHEDULE_ID,LICENSE_TYPE,FEE_TYPE,FEE_AMOUNT," +
        "  EFFECTIVE_DATE,EXPIRY_DATE,CURRENCY_CODE,DESCRIPTION,ACTIVE_STATUS " +
        "FROM DLIS.LICENSE_FEE_SCHEDULE " +
        "WHERE LICENSE_TYPE=? AND FEE_TYPE=? AND ACTIVE_STATUS='A' " +
        "  AND EFFECTIVE_DATE <= CURRENT DATE " +
        "  AND (EXPIRY_DATE IS NULL OR EXPIRY_DATE >= CURRENT DATE) " +
        "FETCH FIRST 1 ROWS ONLY";

    @Override
    public Optional<LicenseFeeSchedule> findActiveFee(String licenseType, String feeType) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_ACTIVE_FEE)) {
            ps.setString(1, licenseType);
            ps.setString(2, feeType);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    LicenseFeeSchedule f = new LicenseFeeSchedule();
                    f.setFeeScheduleId(rs.getLong("FEE_SCHEDULE_ID"));
                    f.setLicenseType(rs.getString("LICENSE_TYPE"));
                    f.setFeeType(rs.getString("FEE_TYPE"));
                    f.setFeeAmount(rs.getBigDecimal("FEE_AMOUNT"));
                    f.setEffectiveDate(rs.getDate("EFFECTIVE_DATE").toLocalDate());
                    Date ed = rs.getDate("EXPIRY_DATE");
                    f.setExpiryDate(ed != null ? ed.toLocalDate() : null);
                    f.setCurrencyCode(rs.getString("CURRENCY_CODE"));
                    f.setDescription(rs.getString("DESCRIPTION"));
                    f.setActiveStatus(rs.getString("ACTIVE_STATUS"));
                    return Optional.of(f);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("LicenseFeeScheduleDao.findActiveFee failed", e);
        }
        return Optional.empty();
    }
}
