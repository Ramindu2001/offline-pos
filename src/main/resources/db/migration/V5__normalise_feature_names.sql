-- ===========================================================================
-- V5  normalise two feature names
--
-- `sysfeatures` rows 59 and 64 carry a non-breaking space (U+00A0, bytes C2 A0)
-- where a plain space belongs - "Expired Items" and "Monthly Sale". It came in
-- with the legacy dump and survived the V3 seed unchanged.
--
-- It looks like an ordinary space in most fonts, which is what makes it worth
-- fixing rather than living with: the role-permissions screen built in Phase 6
-- lists every feature by name, and a search or a sort on that list would quietly
-- miss these two. Some terminals and report exports also render it as a question
-- mark.
--
-- V3 has shipped and a migration that has shipped is never edited, so the
-- correction is its own file.
-- ===========================================================================

UPDATE `sysfeatures`
   SET `FeatureName` = REPLACE(`FeatureName`, CHAR(0xC2, 0xA0 USING utf8mb4), ' ')
 WHERE `FeatureName` LIKE CONCAT('%', CHAR(0xC2, 0xA0 USING utf8mb4), '%');
