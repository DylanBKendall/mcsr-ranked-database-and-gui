-- MCSR Project Schema
-- Creates all tables for the database

CREATE DATABASE IF NOT EXISTS mcsr;
USE mcsr;

-- mcsr.matches definition

CREATE TABLE `matches` (
  `id` int NOT NULL,
  `type` tinyint DEFAULT NULL,
  `category` varchar(32) DEFAULT NULL,
  `gameMode` varchar(32) DEFAULT NULL,
  `season` tinyint DEFAULT NULL,
  `date` int DEFAULT NULL,
  `seed_id` varchar(64) DEFAULT NULL,
  `result_uuid` varchar(64) DEFAULT NULL,
  `result_time` int DEFAULT NULL,
  `forfeited` tinyint DEFAULT NULL,
  `decayed` tinyint DEFAULT NULL,
  `beginner` tinyint DEFAULT NULL,
  `tag` varchar(64) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_matches_season` (`season`),
  KEY `idx_matches_seedid` (`seed_id`),
  KEY `idx_matches_resultuuid` (`result_uuid`),
  CONSTRAINT `fk_matches_result_user` FOREIGN KEY (`result_uuid`) REFERENCES `users` (`uuid`),
  CONSTRAINT `fk_matches_seed` FOREIGN KEY (`seed_id`) REFERENCES `seeds` (`seed_id`),
  CONSTRAINT `fk_matches_seeds` FOREIGN KEY (`seed_id`) REFERENCES `seeds` (`seed_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- mcsr.seeds definition

CREATE TABLE `seeds` (
  `seed_id` varchar(64) NOT NULL,
  `seed_overworld` varchar(64) DEFAULT NULL,
  `seed_nether` varchar(64) DEFAULT NULL,
  `seedType` varchar(64) DEFAULT NULL,
  `bastionType` varchar(64) DEFAULT NULL,
  PRIMARY KEY (`seed_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- mcsr.seed_variations definition

CREATE TABLE `seed_variations` (
  `seed_id` varchar(64) NOT NULL,
  `var_index` tinyint NOT NULL,
  `variation` varchar(255) NOT NULL,
  PRIMARY KEY (`seed_id`,`var_index`),
  CONSTRAINT `seed_variations_ibfk_1` FOREIGN KEY (`seed_id`) REFERENCES `seeds` (`seed_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- mcsr.seed_end_towers definition

CREATE TABLE `seed_end_towers` (
  `seed_id` varchar(64) NOT NULL,
  `tower_index` tinyint NOT NULL,
  `height` int NOT NULL,
  PRIMARY KEY (`seed_id`,`tower_index`),
  CONSTRAINT `seed_end_towers_ibfk_1` FOREIGN KEY (`seed_id`) REFERENCES `seeds` (`seed_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- mcsr.users definition

CREATE TABLE `users` (
  `uuid` varchar(64) NOT NULL,
  `nickname` varchar(64) DEFAULT NULL,
  `roleType` int DEFAULT NULL,
  `eloRate` int DEFAULT NULL,
  `eloRank` int DEFAULT NULL,
  `country` char(2) DEFAULT NULL,
  `firstOnline` bigint DEFAULT NULL,
  `lastOnline` bigint DEFAULT NULL,
  `lastRanked` bigint DEFAULT NULL,
  `nextDecay` bigint DEFAULT NULL,
  `total_bestTime_ranked` int DEFAULT NULL,
  `total_playedMatches_ranked` int DEFAULT NULL,
  `total_wins_ranked` int DEFAULT NULL,
  `total_loses_ranked` int DEFAULT NULL,
  `total_completions_ranked` int DEFAULT NULL,
  `total_playtime_ranked` bigint DEFAULT NULL,
  `total_forfeits_ranked` int DEFAULT NULL,
  `total_highestWinStreak_ranked` int DEFAULT NULL,
  `total_currentWinStreak_ranked` int DEFAULT NULL,
  `seasonResult_last_eloRate` int DEFAULT NULL,
  `seasonResult_last_eloRank` int DEFAULT NULL,
  `seasonResult_last_phasePoint` int DEFAULT NULL,
  `seasonResult_highest` int DEFAULT NULL,
  `seasonResult_lowest` int DEFAULT NULL,
  PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- mcsr.user_achievements definition

CREATE TABLE `user_achievements` (
  `uuid` varchar(64) NOT NULL,
  `achievement_id` varchar(64) NOT NULL,
  `achievement_date` bigint NOT NULL,
  `level` int DEFAULT NULL,
  `value` bigint DEFAULT NULL,
  `goal` bigint DEFAULT NULL,
  `is_displayed` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`uuid`,`achievement_id`,`achievement_date`),
  CONSTRAINT `user_achievements_ibfk_1` FOREIGN KEY (`uuid`) REFERENCES `users` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- mcsr.user_achievement_data definition

CREATE TABLE `user_achievement_data` (
  `uuid` varchar(64) NOT NULL,
  `achievement_id` varchar(64) NOT NULL,
  `achievement_date` bigint NOT NULL,
  `data_index` int NOT NULL,
  `data_value` varchar(64) NOT NULL,
  PRIMARY KEY (`uuid`,`achievement_id`,`achievement_date`,`data_index`),
  CONSTRAINT `user_achievement_data_ibfk_1` FOREIGN KEY (`uuid`, `achievement_id`, `achievement_date`) REFERENCES `user_achievements` (`uuid`, `achievement_id`, `achievement_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- mcsr.user_season_ranked_stats definition

CREATE TABLE `user_season_ranked_stats` (
  `uuid` varchar(64) NOT NULL,
  `season` tinyint NOT NULL,
  `bestTime_ranked` int DEFAULT NULL,
  `playedMatches_ranked` int DEFAULT NULL,
  `wins_ranked` int DEFAULT NULL,
  `loses_ranked` int DEFAULT NULL,
  `completions_ranked` int DEFAULT NULL,
  `playtime_ranked` bigint DEFAULT NULL,
  `forfeits_ranked` int DEFAULT NULL,
  `highestWinStreak_ranked` int DEFAULT NULL,
  `currentWinStreak_ranked` int DEFAULT NULL,
  PRIMARY KEY (`uuid`,`season`),
  CONSTRAINT `user_season_ranked_stats_ibfk_1` FOREIGN KEY (`uuid`) REFERENCES `users` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- mcsr.user_connections definition

CREATE TABLE `user_connections` (
  `uuid` varchar(64) NOT NULL,
  `connection_type` enum('discord','youtube','twitch') NOT NULL,
  `connection_id` varchar(128) DEFAULT NULL,
  `connection_name` varchar(128) DEFAULT NULL,
  PRIMARY KEY (`uuid`,`connection_type`),
  CONSTRAINT `user_connections_ibfk_1` FOREIGN KEY (`uuid`) REFERENCES `users` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- mcsr.match_players definition

CREATE TABLE `match_players` (
  `match_id` int NOT NULL,
  `uuid` varchar(64) NOT NULL,
  `nickname_at_match` varchar(64) DEFAULT NULL,
  `roleType_at_match` tinyint DEFAULT NULL,
  `eloRate_before` int DEFAULT NULL,
  `eloRank_before` int DEFAULT NULL,
  `country_at_match` char(2) DEFAULT NULL,
  `elo_change` int DEFAULT NULL,
  PRIMARY KEY (`match_id`,`uuid`),
  KEY `idx_match_players_uuid_match` (`uuid`,`match_id`),
  CONSTRAINT `match_players_ibfk_1` FOREIGN KEY (`match_id`) REFERENCES `matches` (`id`),
  CONSTRAINT `match_players_ibfk_2` FOREIGN KEY (`uuid`) REFERENCES `users` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

