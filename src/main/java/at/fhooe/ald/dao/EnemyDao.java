package at.fhooe.ald.dao;

import at.fhooe.ald.model.Enemy;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public interface EnemyDao {
    List<Enemy> findAll() throws SQLException;

    List<Enemy> findByFloorNumber(int floorNumber) throws SQLException;

    Optional<Enemy> findById(int id) throws SQLException;
}
