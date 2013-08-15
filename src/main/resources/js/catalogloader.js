(function($, w) {
	"use strict";
	
	// on document ready
	$(function() {
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
		};
		
		$('#catalogForm input[type="radio"]').change(function() {
			// clear previous tree
			if (treeContainer.children('ul').length > 0)
				treeContainer.dynatree('destroy');
			treeContainer.empty();
			
			// create new tree
			var catalogId = $('#catalogForm input[type="radio"]:checked').val();
			$.get('/plugin/catalog/preview/' + catalogId, function(data) {
				var nodes = [];
				createTreeConfig(data.root, nodes);
				treeContainer.dynatree({'minExpandLevel': 2, 'children': nodes});
			});
		});
		$('#catalogForm input[type="radio"]').change();
	});
}($, window.top));