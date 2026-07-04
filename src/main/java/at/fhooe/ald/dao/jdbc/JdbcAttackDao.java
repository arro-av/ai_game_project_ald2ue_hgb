package at.fhooe.ald.dao.jdbc;

import at.fhooe.ald.dao.AttackDao;
import at.fhooe.ald.model.Attack;
import at.fhooe.ald.model.AttackEffect;
import at.fhooe.ald.model.TargetType;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcAttackDao implements AttackDao {
    private final Database database;

    public JdbcAttackDao(Database database) {
        this.database = database;
    }

    @Override
    public List<Attack> findAll() throws SQLException {
        String sql = """
                SELECT id, name, description, min_damage, max_damage, target_type, effect, cooldown, NULL AS unlock_state
                FROM attacks
                ORDER BY id
                """;
        try (var connection = database.getConnection();
             var statement = connection.prepareStatement(sql);
             var resultSet = statement.executeQuery()) {
            return mapMany(resultSet);
        }
    }

    @Override
    public Optional<Attack> findById(int id) throws SQLException {
        String sql = """
                SELECT id, name, description, min_damage, max_damage, target_type, effect, cooldown, NULL AS unlock_state
                FROM attacks
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

    @Override
    public List<Attack> findByCharacterId(int characterId) throws SQLException {
        String sql = """
                SELECT a.id, a.name, a.description, a.min_damage, a.max_damage, a.target_type, a.effect, a.cooldown,
                       NULL AS unlock_state
                FROM attacks a
                JOIN character_attacks ca ON ca.attack_id = a.id
                WHERE ca.character_id = ?
                ORDER BY ca.slot
                """;
        try (var connection = database.getConnection();
             var statement = connection.prepareStatement(sql)) {
            statement.setInt(1, characterId);
            try (var resultSet = statement.executeQuery()) {
                return mapMany(resultSet);
            }
        }
    }

    @Override
    public List<Attack> findByEnemyId(int enemyId) throws SQLException {
        String sql = """
                SELECT a.id, a.name, a.description, a.min_damage, a.max_damage, a.target_type, a.effect, a.cooldown,
                       ea.unlock_state
                FROM attacks a
                JOIN enemy_attacks ea ON ea.attack_id = a.id
                WHERE ea.enemy_id = ?
                ORDER BY ea.slot
                """;
        try (var connection = database.getConnection();
             var statement = connection.prepareStatement(sql)) {
            statement.setInt(1, enemyId);
            try (var resultSet = statement.executeQuery()) {
                return mapMany(resultSet);
            }
        }
    }

    private List<Attack> mapMany(ResultSet resultSet) throws SQLException {
        List<Attack> attacks = new ArrayList<>();
        while (resultSet.next()) {
            attacks.add(map(resultSet));
        }
        return attacks;
    }

    private Attack map(ResultSet resultSet) throws SQLException {
        return new Attack(
                resultSet.getInt("id"),
                resultSet.getString("name"),
                resultSet.getString("description"),
                resultSet.getInt("min_damage"),
                resultSet.getInt("max_damage"),
                TargetType.valueOf(resultSet.getString("target_type")),
                AttackEffect.valueOf(resultSet.getString("effect")),
                resultSet.getInt("cooldown"),
                resultSet.getString("unlock_state")
        );
    }
}
