<#if enable_spring_ui>
<#include "molgenis-header.ftl">
<#include "molgenis-footer.ftl">
<#assign css=["ui.dynatree.css", "protocolviewer.css", "loaders.css"]>
<#assign js=["jquery-ui-1.9.2.custom.min.js", "jquery.dynatree.min.js", "studydefinitionmanager.js"]>
<@header css js/>
	<div class="span2"></div>
	<div class="span8">
	<#if errorMessage??>
		<div class="alert alert-error">
			<button type="button" class="close" data-dismiss="alert">&times;</button>
			${errorMessage}
		</div>
	</#if>
	<#if successMessage??>
		<div class="alert alert-success">
			<button type="button" class="close" data-dismiss="alert">&times;</button>
			${successMessage}
		</div>
	</#if>		
	<#if studyDefinitions??>
		<div class="row-fluid">	
			<div class="well">
				<p id="loader-title" class="box-title">Choose a study definition to load</p>
			<#if studyDefinitions?size == 0>
				<p>No studydefinitions found</p>
			<#else>
				<form id="studyDefinitionForm" name="studyDefinitionForm" method="post" action="${context_url}/load" onsubmit="parent.showSpinner(); return true;">
					<div class="row-fluid">
						<div class="span6">
							<div id="resultsTable">
								<table class="table table-striped table-hover listtable selection-table">
									<thead>
										<tr>
											<th></th>
											<th>Id</th>
											<th>Name</th>
										</tr>
									</thead>
									<#assign foundStudyDefinition = false>
									<tbody>
									<#list studyDefinitions as studyDefinition>
										<tr>
											<td class="listEntryRadio">
											<#if studyDefinition.loaded>
												LOADED
											<#else>
												<input id="catalog_${studyDefinition.id}" type="radio" name="id" value="${studyDefinition.id}" <#if !foundStudyDefinition>checked<#assign foundStudyDefinition = true></#if> >
											</#if>
											</td>
											<td class="listEntryId">
												<label for="catalog_${studyDefinition.id}">${studyDefinition.id}</label>
											</td>
											<td>
												<label for="catalog_${studyDefinition.id}">${studyDefinition.name}</label>
											</td>
										</tr>
									</#list>
									</tbody>
								</table>
							</div>
						<#if foundStudyDefinition>
							<input id="submitButton" type="submit" class="btn pull-right" value="Load" />
						</#if>
						</div>
						<div class="span6" id="study-definition-info">
							<ul class="nav nav-tabs">
								<li class="active"><a href="#study-definition-viewer" class="active" data-toggle="tab">Details</a></li>
								<li><a href="#study-definition-editor" class="active" data-toggle="tab">Manage</a></li>
							</ul>
							<div class="tab-content">
							    <div class="tab-pane active" id="study-definition-viewer">
									<div id="study-definition-viewer-info">
									</div>
									<div id="study-definition-viewer-tree">
									</div>
							    </div>
							    <div class="tab-pane" id="study-definition-editor">
							    	<div id="study-definition-editor-info">
									</div>
									<div id="study-definition-editor-tree">
									</div>
									<input id="updateButton" type="submit" class="btn pull-right" value="Save" />
							    </div>
							</div>
						</div>
					</div>
				</form>
			</#if>
			</div>
		</div>
	</#if>
	</div>
	<div class="span2"></div>
<@footer/>
<#else>
<!DOCTYPE html>
<html>
	<head>
		<title>Studydefinition loader plugin</title>
		<meta charset="utf-8">
		<link rel="stylesheet" href="/css/jquery-ui-1.9.2.custom.min.css" type="text/css">
		<link rel="stylesheet" href="/css/bootstrap.min.css" type="text/css">
		<link rel="stylesheet" href="/css/ui.dynatree.css" type="text/css">
		<link rel="stylesheet" href="/css/protocolviewer.css" type="text/css">
		<link rel="stylesheet" href="/css/loaders.css" type="text/css">
		<script type="text/javascript" src="/js/jquery-1.8.3.min.js"></script>
		<script type="text/javascript" src="/js/jquery-ui-1.9.2.custom.min.js"></script>
		<script type="text/javascript" src="/js/bootstrap.min.js"></script>
		<script type="text/javascript" src="/js/jquery.dynatree.min.js"></script>
		<script type="text/javascript" src="/js/molgenis-all.js"></script>
		<script type="text/javascript" src="/js/studydefinitionmanager.js"></script>
		<script type="text/javascript">
			$(function() {
				parent.hideSpinner();
				
				$('#studyDefinitionForm').submit(function() {
					$('#submitButton').attr("disabled", "disabled");
					parent.showSpinner(); 
					return true;
				});
			});
		</script>
	</head>
	<body>
		<div class="container-fluid">
			<div class="row-fluid">
				<div class="span2"></div>
				<div class="span8">
				<#if errorMessage??>
					<div class="alert alert-error">
						<button type="button" class="close" data-dismiss="alert">&times;</button>
						${errorMessage}
					</div>
				</#if>
				<#if successMessage??>
					<div class="alert alert-success">
						<button type="button" class="close" data-dismiss="alert">&times;</button>
						${successMessage}
					</div>
				</#if>		
				<#if studyDefinitions??>
					<div class="row-fluid">	
						<div class="well">
							<p id="loader-title" class="box-title">Choose a study definition to load</p>
						<#if studyDefinitions?size == 0>
							<p>No studydefinitions found</p>
						<#else>
							<form id="studyDefinitionForm" name="studyDefinitionForm" method="post" action="/plugin/studydefinition/load" onsubmit="parent.showSpinner(); return true;">
								<div class="row-fluid">
									<div class="span6">
										<div id="resultsTable">
											<table class="table table-striped table-hover listtable selection-table">
												<thead>
													<tr>
														<th></th>
														<th>Id</th>
														<th>Name</th>
													</tr>
												</thead>
												<#assign foundStudyDefinition = false>
												<tbody>
												<#list studyDefinitions as studyDefinition>
													<tr>
														<td class="listEntryRadio">
														<#if studyDefinition.loaded>
															LOADED
														<#else>
															<input id="catalog_${studyDefinition.id}" type="radio" name="id" value="${studyDefinition.id}" <#if !foundStudyDefinition>checked<#assign foundStudyDefinition = true></#if> >
														</#if>
														</td>
														<td class="listEntryId">
															<label for="catalog_${studyDefinition.id}">${studyDefinition.id}</label>
														</td>
														<td>
															<label for="catalog_${studyDefinition.id}">${studyDefinition.name}</label>
														</td>
													</tr>
												</#list>
												</tbody>
											</table>
										</div>
									<#if foundStudyDefinition>
										<input id="submitButton" type="submit" class="btn pull-right" value="Load" />
									</#if>
									</div>
									<div class="span6" id="study-definition-info">
										<ul class="nav nav-tabs">
											<li class="active"><a href="#study-definition-viewer" class="active" data-toggle="tab">Details</a></li>
											<li><a href="#study-definition-editor" class="active" data-toggle="tab">Manage</a></li>
										</ul>
										<div class="tab-content">
										    <div class="tab-pane active" id="study-definition-viewer">
												<div id="study-definition-viewer-info">
												</div>
												<div id="study-definition-viewer-tree">
												</div>
										    </div>
										    <div class="tab-pane" id="study-definition-editor">
										    	<div id="study-definition-editor-info">
												</div>
												<div id="study-definition-editor-tree">
												</div>
												<input id="updateButton" type="submit" class="btn pull-right" value="Save" />
										    </div>
										</div>
									</div>
								</div>
							</form>
						</#if>
						</div>
					</div>
				</#if>
				</div>
				<div class="span2"></div>
			</div>
		</div>	
	</body>
</html>
</#if>