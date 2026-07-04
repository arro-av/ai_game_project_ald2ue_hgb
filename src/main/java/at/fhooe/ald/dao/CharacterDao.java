package at.fhooe.ald.dao;

import at.fhooe.ald.model.PlayerCharacter;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public interface CharacterDao {
    List<PlayerCharacter> findAll() throws SQLException;

    List<PlayerCharacter> findAvailableForFloor(int floorNumber) throws SQLException;

    Optional<PlayerCharacter> findById(int id) throws SQLException;
}
