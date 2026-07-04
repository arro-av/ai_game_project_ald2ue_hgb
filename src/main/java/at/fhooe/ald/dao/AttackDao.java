package at.fhooe.ald.dao;

import at.fhooe.ald.model.Attack;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public interface AttackDao {
    List<Attack> findAll() throws SQLException;

    Optional<Attack> findById(int id) throws SQLException;

    List<Attack> findByCharacterId(int characterId) throws SQLException;

    List<Attack> findByEnemyId(int enemyId) throws SQLException;
}
