package at.fhooe.ald.dao.jdbc;

import at.fhooe.ald.dao.DialogueDao;
import at.fhooe.ald.model.DialogueLine;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class JdbcDialogueDao implements DialogueDao {
    private final Database database;

    public JdbcDialogueDao(Database database) {
        this.database = database;
    }

    @Override
    public List<DialogueLine> findByFloorNumber(int floorNumber) throws SQLException {
        String sql = """
                SELECT id, floor_number, speaker, text, display_order
                FROM dialogue_lines
                WHERE floor_number = ?
                ORDER BY display_order
                """;
        try (var connection = database.getConnection();
             var statement = connection.prepareStatement(sql)) {
            statement.setInt(1, floorNumber);
            try (var resultSet = statement.executeQuery()) {
                List<DialogueLine> lines = new ArrayList<>();
                while (resultSet.next()) {
                    lines.add(new DialogueLine(
                            resultSet.getInt("id"),
                            resultSet.getInt("floor_number"),
                            resultSet.getString("speaker"),
                            resultSet.getString("text"),
                            resultSet.getInt("display_order")
                    ));
                }
                return lines;
            }
        }
    }
}
