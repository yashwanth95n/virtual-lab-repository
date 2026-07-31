-- MySQL dump 10.13  Distrib 8.0.45, for Win64 (x86_64)
--
-- Host: localhost    Database: cyberlab
-- ------------------------------------------------------
-- Server version	8.0.45

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `users`
--

DROP TABLE IF EXISTS `users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `users` (
  `id` int NOT NULL AUTO_INCREMENT,
  `full_name` varchar(100) NOT NULL,
  `email` varchar(100) NOT NULL,
  `password` varchar(255) NOT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `email` (`email`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `users`
--

LOCK TABLES `users` WRITE;
/*!40000 ALTER TABLE `users` DISABLE KEYS */;
INSERT INTO `users` VALUES (1,'Yash','yashwanth95n@gmail.com','$2b$10$ZwUzBrClLlhwOciNOAZfYOCquFXM1dMTR.P2INzA6V7085n1/RiWC','2026-07-28 03:41:54'),(2,'eswar','eswarkp@gmail.com','$2b$10$JMJq.jTyV2jTqkCXRRL/MuIYhlTeYAvLCMWhO.3s.D7hFEMBrD4jK','2026-07-28 03:50:18'),(3,'srikar','srikar@gmail.com','$2b$10$ADQ2nIX8cW.ivpIkihkj5e5aDZRg10BDaZNZCiqTNihkXMljvA0QK','2026-07-28 04:25:40'),(4,'Srija','srija@gmail.com','$2b$10$VhJF4fH/p9835XKgiGWHcenkhbx.wWBPWyfb9ub.vVyOdhOFgm6y.','2026-07-30 09:26:38'),(5,'Yagnesh','yagnesh@gmail.com','$2b$10$vplUR6POlEIkwqywblZRCuz9PO4UnEz4s7fOJKd8O3XLXxVHc5tn2','2026-07-30 11:49:32'),(6,'manoj','manoj@gmail.com','$2b$10$XRHk9N138iVy0vFl5KPNPekX0w8OS9cq1xlNQl4Mv9cYEiZVRHb7m','2026-07-30 12:37:41'),(7,'qwer','q@gmail.com','$2b$10$vcUta7M6j2uHzsbOJKmd3eMAgqZs.29yUdUL8U08SvQEoHxjy1HNq','2026-07-30 12:39:07');
/*!40000 ALTER TABLE `users` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-07-30 18:24:12
