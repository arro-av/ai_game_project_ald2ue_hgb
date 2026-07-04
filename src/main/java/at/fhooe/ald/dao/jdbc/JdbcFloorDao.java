package at.fhooe.ald.dao.jdbc;

import at.fhooe.ald.dao.EnemyDao;
import at.fhooe.ald.dao.FloorDao;
import at.fhooe.ald.model.Floor;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcFloorDao implements FloorDao {
    private final Database database;
    private final EnemyDao enemyDao;

    public JdbcFloorDao(Database database, EnemyDao enemyDao) {
        this.database = database;
        this.enemyDao = enemyDao;
    }

    @Override
    public List<Floor> findAll() throws SQLException {
        String sql = """
                SELECT id, floor_number, name, background_path
                FROM floors
                ORDER BY floor_number
                """;
        try (var connection = database.getConnection();
             var statement = connection.prepareStatement(sql);
             var resultSet = statement.executeQuery()) {
            List<Floor> floors = new ArrayList<>();
            while (resultSet.next()) {
                floors.add(map(resultSet));
            }
            return floors;
        }
    }

    @Override
    public Optional<Floor> findByNumber(int floorNumber) throws SQLException {
        String sql = """
                SELECT id, floor_number, name, background_path
                FROM floors
                WHERE floor_number = ?
                """;
        try (var connection = database.getConnection();
             var statement = connection.prepareStatement(sql)) {
            statement.setInt(1, floorNumber);
            try (var resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(map(resultSet));
                }
                return Optional.empty();
            }
        }
    }

    private Floor map(ResultSet resultSet) throws SQLException {
        int floorNumber = resultSet.getInt("floor_number");
        return new Floor(
                resultSet.getInt("id"),
                floorNumber,
                resultSet.getString("name"),
                resultSet.getString("background_path"),
                enemyDao.findByFloorNumber(floorNumber)
        );
    }
}
