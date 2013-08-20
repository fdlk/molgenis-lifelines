(function($, w) {
	"use strict";
	
	// on document ready
	$(function() {
		var infoContainer = $('#catalog-preview-info');
		var treeContainer = $('#catalog-preview-tree');
		
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
		}
		
		$('#catalogForm input[type="radio"]').change(function() {
			if($(this).data('loaded'))
				$('#loadButton').attr('disabled', 'disabled');
			else
				$('#loadButton').removeAttr('disabled');
			
			// clear previous tree
			if (treeContainer.children('ul').length > 0)
				treeContainer.dynatree('destroy');
			infoContainer.empty();
			treeContainer.empty();
			infoContainer.html('Loading preview ...');
			
			// create new catalog preview
			var catalogId = $('#catalogForm input[type="radio"]:checked').val();
			$.get('/plugin/catalog/preview/' + catalogId, function(data) {
				var items= [];
				items.push('<table class="table table-condensed table-borderless">');
				items.push('<tr><td>Version</td><td>' + data.version + '</td></tr>');
				items.push('<tr><td>Description</td><td>' + data.description + '</td></tr>');
				items.push('<tr><td>Authors</td><td>' + data.authors.join(', ') + '</td></tr>');
				items.push('</table>');
				infoContainer.html(items.join(''));
				
				// create new tree
				var nodes = [];
				createTreeConfig(data.root, nodes);
				treeContainer.empty();
				treeContainer.dynatree({'minExpandLevel': 2, 'children': nodes});
			});
		});
		$('#catalogForm input[type="radio"]:checked').change();
	});
}($, window.top));