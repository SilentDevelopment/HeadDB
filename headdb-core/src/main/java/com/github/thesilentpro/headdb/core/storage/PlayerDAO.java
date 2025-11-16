package com.github.thesilentpro.headdb.core.storage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PlayerDAO {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlayerDAO.class);

    public void createTable() {
        try (Connection conn = PlayerStorage.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(SqlUtils.CREATE_TABLE);
        } catch (SQLException ex) {
            LOGGER.error("Failed to create table", ex);
        }
    }

    public void saveAllPlayers(Map<UUID, PlayerData> dataMap) {
        if (dataMap.isEmpty()) {
            return;
        }
        
        try (Connection conn = PlayerStorage.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SqlUtils.INSERT_OR_REPLACE)) {

            for (PlayerData data : dataMap.values()) {
                stmt.setString(1, data.getUniqueId().toString());
                stmt.setString(2, data.getLanguage());

                List<Integer> favs = data.getFavorites();
                String favorites = (favs == null || favs.isEmpty()) ? "" : 
                        favs.stream().map(String::valueOf).collect(Collectors.joining(","));
                stmt.setString(3, favorites);

                List<UUID> localFavs = data.getLocalFavorites();
                String localFavorites = (localFavs == null || localFavs.isEmpty()) ? "" :
                        localFavs.stream().map(UUID::toString).collect(Collectors.joining(","));

                stmt.setString(4, localFavorites);
                stmt.setInt(5, data.isSoundEnabled() ? 1 : 0);

                stmt.addBatch();
            }

            stmt.executeBatch();

        } catch (SQLException ex) {
            LOGGER.error("Failed to save players", ex);
        }
    }

    public Map<UUID, PlayerData> loadAllPlayers() {
        Map<UUID, PlayerData> dataMap = new HashMap<>();

        try (Connection conn = PlayerStorage.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(SqlUtils.SELECT_ALL)) {

            while (rs.next()) {
                UUID uuid = UUID.fromString(rs.getString("uuid"));
                String lang = rs.getString("language");
                boolean sound = rs.getInt("sound_enabled") == 1;
                
                String favs = rs.getString("favorites");
                List<Integer> favorites = (favs == null || favs.isEmpty())
                        ? new ArrayList<>()
                        : Arrays.stream(favs.split(","))
                                .map(Integer::parseInt)
                                .collect(Collectors.toCollection(ArrayList::new));
                
                String localFavs = rs.getString("local_favorites");
                List<UUID> localFavorites = (localFavs == null || localFavs.isEmpty())
                        ? new ArrayList<>()
                        : Arrays.stream(localFavs.split(","))
                                .map(UUID::fromString)
                                .collect(Collectors.toCollection(ArrayList::new));

                dataMap.put(uuid, new PlayerData(uuid, lang, sound, favorites, localFavorites));
            }

        } catch (SQLException ex) {
            LOGGER.error("Failed to load players", ex);
        }

        return dataMap;
    }
}
