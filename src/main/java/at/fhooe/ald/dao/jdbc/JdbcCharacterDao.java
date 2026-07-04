package at.fhooe.ald.dao.jdbc;

import at.fhooe.ald.dao.AttackDao;
import at.fhooe.ald.dao.CharacterDao;
import at.fhooe.ald.model.PlayerCharacter;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcCharacterDao implements CharacterDao {
    private final Database database;
    private final AttackDao attackDao;

    public JdbcCharacterDao(Database database, AttackDao attackDao) {
        this.database = database;
        this.attackDao = attackDao;
    }

    @Override
    public List<PlayerCharacter> findAll() throws SQLException {
        String sql = """
                SELECT id, name, max_hp, speed, sprite_path, portrait_path, join_floor
                FROM characters
                ORDER BY id
                """;
        try (var connection = database.getConnection();
             var statement = connection.prepareStatement(sql);
             var resultSet = statement.executeQuery()) {
            return mapMany(resultSet);
        }
    }

    @Override
    public List<PlayerCharacter> findAvailableForFloor(int floorNumber) throws SQLException {
        String sql = """
                SELECT id, name, max_hp, speed, sprite_path, portrait_path, join_floor
                FROM characters
                WHERE join_floor <= ?
                ORDER BY id
                """;
        try (var connection = database.getConnection();
             var statement = connection.prepareStatement(sql)) {
            statement.setInt(1, floorNumber);
            try (var resultSet = statement.executeQuery()) {
                return mapMany(resultSet);
            }
        }
    }

    @Override
    public Optional<PlayerCharacter> findById(int id) throws SQLException {
        String sql = """
                SELECT id, name, max_hp, speed, sprite_path, portrait_path, join_floor
                FROM characters
                WHERE id = ?
                """;
        try (var connection = database.getConnection();
             var statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (var resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(map(resultSet));
                }
                return Optional.empty();
            }
        }
    }

    private List<PlayerCharacter> mapMany(ResultSet resultSet) throws SQLException {
        List<PlayerCharacter> characters = new ArrayList<>();
        while (resultSet.next()) {
            characters.add(map(resultSet));
        }
        return characters;
    }

    private PlayerCharacter map(ResultSet resultSet) throws SQLException {
        int id = resultSet.getInt("id");
        int maxHp = resultSet.getInt("max_hp");
        return new PlayerCharacter(
                id,
                resultSet.getString("name"),
                maxHp,
                maxHp,
                resultSet.getInt("speed"),
                resultSet.getString("sprite_path"),
                resultSet.getString("portrait_path"),
                resultSet.getInt("join_floor"),
                attackDao.findByCharacterId(id)
        );
    }
}
