package at.fhooe.ald.dao.jdbc;

import at.fhooe.ald.dao.AttackDao;
import at.fhooe.ald.dao.EnemyDao;
import at.fhooe.ald.model.BossEnemy;
import at.fhooe.ald.model.Enemy;
import at.fhooe.ald.model.EnemyType;
import at.fhooe.ald.model.TrashEnemy;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcEnemyDao implements EnemyDao {
    private final Database database;
    private final AttackDao attackDao;

    public JdbcEnemyDao(Database database, AttackDao attackDao) {
        this.database = database;
        this.attackDao = attackDao;
    }

    @Override
    public List<Enemy> findAll() throws SQLException {
        String sql = """
                SELECT id, name, enemy_type, max_hp, speed, sprite_path, passive_name, passive_description
                FROM enemies
                ORDER BY id
                """;
        try (var connection = database.getConnection();
             var statement = connection.prepareStatement(sql);
             var resultSet = statement.executeQuery()) {
            return mapMany(resultSet);
        }
    }

    @Override
    public List<Enemy> findByFloorNumber(int floorNumber) throws SQLException {
        String sql = """
                SELECT e.id, e.name, e.enemy_type, e.max_hp, e.speed, e.sprite_path, e.passive_name, e.passive_description,
                       fe.quantity
                FROM enemies e
                JOIN floor_enemies fe ON fe.enemy_id = e.id
                JOIN floors f ON f.id = fe.floor_id
                WHERE f.floor_number = ?
                ORDER BY e.enemy_type, e.id
                """;
        try (var connection = database.getConnection();
             var statement = connection.prepareStatement(sql)) {
            statement.setInt(1, floorNumber);
            try (var resultSet = statement.executeQuery()) {
                List<Enemy> enemies = new ArrayList<>();
                while (resultSet.next()) {
                    int quantity = resultSet.getInt("quantity");
                    for (int i = 0; i < quantity; i++) {
                        enemies.add(map(resultSet));
                    }
                }
                return enemies;
            }
        }
    }

    @Override
    public Optional<Enemy> findById(int id) throws SQLException {
        String sql = """
                SELECT id, name, enemy_type, max_hp, speed, sprite_path, passive_name, passive_description
                FROM enemies
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

    private List<Enemy> mapMany(ResultSet resultSet) throws SQLException {
        List<Enemy> enemies = new ArrayList<>();
        while (resultSet.next()) {
            enemies.add(map(resultSet));
        }
        return enemies;
    }

    private Enemy map(ResultSet resultSet) throws SQLException {
        int id = resultSet.getInt("id");
        int maxHp = resultSet.getInt("max_hp");
        EnemyType enemyType = EnemyType.valueOf(resultSet.getString("enemy_type"));
        if (enemyType == EnemyType.BOSS) {
            return new BossEnemy(
                    id,
                    resultSet.getString("name"),
                    maxHp,
                    maxHp,
                    resultSet.getInt("speed"),
                    resultSet.getString("sprite_path"),
                    resultSet.getString("passive_name"),
                    resultSet.getString("passive_description"),
                    attackDao.findByEnemyId(id)
            );
        }
        return new TrashEnemy(
                id,
                resultSet.getString("name"),
                maxHp,
                maxHp,
                resultSet.getInt("speed"),
                resultSet.getString("sprite_path"),
                resultSet.getString("passive_name"),
                resultSet.getString("passive_description"),
                attackDao.findByEnemyId(id)
        );
    }
}
