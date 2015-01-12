The GenericLayerStudyManagerService converts both StudyDataRequests and StudyDefinitions to HL7 documents and sends them to the server.

The mapping is as follows:
| HL7 QualityMeasureDocument | StudyBean | StudyDataRequest | StudyDefinition |
|------------------------------------------------------------------------|-----------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------|
| title | name | name | name |
| text | createdBy | "Created by "+molgenisUser.userName+" ("+molgenisUser.email+")" | "Created by "+StringUtils.join(studyDefinition.authors,' ')+(" (" + authorEmail +")" |
| component.section.text.list.item | repeat: folder in selectedCatalogFolders | repeat: protocol in protocols (distinct values for observation.code.code) | repeat: item in items (distinct values for observation.code.code) |
| component.section.text.list.item | folder.textCode | ObservationIdConverter.getObservationCode(protocol.identifier) | item.name |
| component.section.entry | repeat:folder in selectedCatalogFolders (distinct values for folder.code) | repeat: protocol in protocols (distinct values for observation.code.code) | repeat: item in items |
| component.section.entry.observation.code |  |  | if either item.code or item.codesystem is null, lookup observableFeature with id = item.id |
| component.section.entry.observation.code.code | folder.code | ObservationIdConverter.getObservationCode(protocol.identifier) | item.code or observableFeature.identifier |
| component.section.entry.observation.code.codeSystem | folder.codeSystem | 2.16.840.1.113883.2.4.3.8.1000.54.8 | item.codeSystem or 2.16.840.1.113883.2.4.3.8.1000.54.8 |
| component.section.entry.observation.code.displayName | folder.displayName | protocol.name | null |
| component.section.entry.observation.sourceOf | repeat: folder in selectedCatalogFolders  (filter for value in folder.code) | repeat: protocol in protocols (filter for value of observation.code.code) | repeat: item in items (filter for value of observation.code.code) |
| component.section.entry.observation.sourceOf.encounter.code.code | folder.measurementCode | id = new OmxCatalogFolder(protocol).getPath().get(2).getExternalId(); MeasurementIdConverter.getMeasurementCode(id); | id = item.path[2].id; MeasurementIdConverter.getMeasurementCode(id) |
| component.section.entry.observation.sourceOf.encounter.code.codeSystem | folder.measurementCodeSystem | MeasurementIdConverter.getMeasurementCodeSystem(id); | MeasurementIdConverter.getMeasurementCodeSystem(id) |