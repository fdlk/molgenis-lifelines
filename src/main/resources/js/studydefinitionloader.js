(function($, w) {
	"use strict";
	
	// on document ready
	$(function() {
		var infoContainer = $('#study-definition-preview-info');
		var treeContainer = $('#study-definition-preview-tree');
		
		function createTreeConfig(node, dynaNode) {
			if(node.items) {
				$.each(node.items, function(idx, item) {
					dynaNode.push({title: item});
				});
			}
			if(node.children) {
				$.each(node.children, function(idx, child) {
					var dynaChild = {title: child.name, isFolder: true, key: child.name, children:[]};
					createTreeConfig(child, dynaChild.children);
					dynaNode.push(dynaChild);
				});
			}
		};
		
		$('#studyDefinitionForm input[type="radio"]').change(function() {
			// clear previous tree
			if (treeContainer.children('ul').length > 0)
				treeContainer.dynatree('destroy');
			infoContainer.empty();
			treeContainer.empty();
			treeContainer.html('Loading preview ...');
			
			// create new tree
			var catalogOfStudyDefinitionId = $('#studyDefinitionForm input[type="radio"]:checked').val();
			$.get('/plugin/studydefinition/preview/' + catalogOfStudyDefinitionId, function(data) {
				var items= [];
				items.push('<table class="table table-condensed table-borderless">');
				items.push('<tr><td>Version</td><td>' + data.version + '</td></tr>');
				items.push('<tr><td>Description</td><td>' + data.description + '</td></tr>');
				items.push('<tr><td>Authors</td><td>' + data.authors.join(', ') + '</td></tr>');
				items.push('</table>');
				infoContainer.html(items.join(''));
				
				var nodes = [];
				createTreeConfig(data.root, nodes);
				treeContainer.empty();
				treeContainer.dynatree({'minExpandLevel': 2, 'children': nodes});
			});
		});
		$('#studyDefinitionForm input[type="radio"]:checked').change();
	});
}($, window.top));