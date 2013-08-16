<!DOCTYPE html>
<html>
	<head>
		<title>Catalog loader plugin</title>
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
		<script type="text/javascript" src="/js/catalogloader.js"></script>
		<script type="text/javascript">
			$(function() {
				parent.hideSpinner();
				
				$('#catalogForm').submit(function() {
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
				<#if catalogs??>	
					<div class="well">
						<div class="row-fluid">
							<p id="loader-title" class="box-title">Choose a catalog to load</p>
						<#if catalogs?size == 0>
							<p>No catalogs found</p>
						<#else>
							<form id="catalogForm" name="catalogForm" method="post" action="/plugin/catalog/load">
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
											<#assign foundCatalog = false>
												<tbody>
											<#list catalogs as catalog>
												<tr>
													<td class="listEntryRadio">
														<input id="catalog_${catalog.id}" type="radio" name="id" value="${catalog.id}" data-loaded="<#if catalog.loaded>true<#else>false</#if>" <#if !foundCatalog>checked<#assign foundCatalog = true></#if> >
													</td>
													<td class="listEntryId">
														<label for="catalog_${catalog.id}">${catalog.id}</label>
													</td>
													<td>
														<label for="catalog_${catalog.id}">${catalog.name}<#if catalog.loaded><input type="submit" name="unload" class="btn btn-small pull-right" value="Unload" /></#if></label>
													</td>
												</tr>
											</#list>
												</tbody>
											</table>
										</div>
									</div>
									<div class="span6">
										<div id="catalog-preview">
											<div id="catalog-preview-tree">
											</div>
										</div>
									</div>
								</div>
							<#if foundCatalog>
								<input id="loadButton" type="submit" name="load" class="btn pull-right" value="Load" />
							</#if>
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