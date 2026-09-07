CREATE DATABASE IF NOT EXISTS `home_library`;
USE `home_library`;

DROP TABLE IF EXISTS `author_book`;
DROP TABLE IF EXISTS `book`;
DROP TABLE IF EXISTS `author`;
DROP TABLE IF EXISTS `subcategory`;
DROP TABLE IF EXISTS `category`;

CREATE TABLE `category`
(
    `id`   bigint       NOT NULL AUTO_INCREMENT,
    `name` varchar(255) NOT NULL,
    PRIMARY KEY (`id`)
);

CREATE TABLE `subcategory`
(
    `id`          bigint        NOT NULL AUTO_INCREMENT,
    `name`        varchar(255)  NOT NULL,
    `category_id` bigint        NOT NULL,
    PRIMARY KEY (`id`),
    FOREIGN KEY (`category_id`) REFERENCES `category` (`id`)
);

CREATE TABLE `author`
(
    `id`         bigint       NOT NULL AUTO_INCREMENT,
    `name`       varchar(255) NOT NULL,
    PRIMARY KEY (`id`)
);

CREATE TABLE `book`
(
    `id`             bigint        NOT NULL AUTO_INCREMENT,
    `priority`       tinyint       DEFAULT 1,
    `title`          varchar(255)  NOT NULL,
    `subcategory_id` bigint        NOT NULL,
    PRIMARY KEY (`id`),
    FOREIGN KEY (`subcategory_id`) REFERENCES `subcategory` (`id`)
);

CREATE TABLE `author_book`
(
    `author_id` bigint        NOT NULL,
    `book_id`   bigint        NOT NULL,
    FOREIGN KEY (`author_id`) REFERENCES `author` (`id`),
    FOREIGN KEY (`book_id`)   REFERENCES `book` (`id`)
);
