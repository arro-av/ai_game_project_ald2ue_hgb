package at.fhooe.ald.dao;

import at.fhooe.ald.model.Floor;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public interface FloorDao {
    List<Floor> findAll() throws SQLException;

    Optional<Floor> findByNumber(int floorNumber) throws SQLException;
}
