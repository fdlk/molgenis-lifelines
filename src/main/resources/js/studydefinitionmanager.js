(function($, w) {
	"use strict";
	
	// on document ready
	$(function() {
		var viewInfoContainer = $('#study-definition-viewer-info');
		var viewTreeContainer = $('#study-definition-viewer-tree');
		var editInfoContainer = $('#study-definition-editor-info');
		var editTreeContainer = $('#study-definition-editor-tree');
		
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
		
		function createTreeConfig2(node, dynaNode) {
			var dynaChild = {key: node.id, title: node.name, select: node.selected, isFolder: true, children:[]};
			dynaNode.push(dynaChild);
			if(node.children) {
				$.each(node.children, function(idx, child) {
					createTreeConfig2(child, dynaChild.children);
				});
			}
			if(node.items) {
				$.each(node.items, function(idx, item) {
					dynaChild.children.push({key: item.id, title: item.name, select: item.selected});
				});
			}
		}
		
		function updateStudyDefinitionViewer() {
			// clear previous tree
			if (viewTreeContainer.children('ul').length > 0)
				viewTreeContainer.dynatree('destroy');
			viewInfoContainer.empty();
			viewTreeContainer.empty();
			viewTreeContainer.html('Loading viewer ...');
			
			// create new tree
			var studyDefinitionId = $('#studyDefinitionForm input[type="radio"]:checked').val();
			$.get('/plugin/studydefinitionmanager/view/' + studyDefinitionId, function(data) {
				var items= [];
				items.push('<table class="table table-condensed table-borderless">');
				items.push('<tr><td>Version</td><td>' + data.version + '</td></tr>');
				items.push('<tr><td>Description</td><td>' + data.description + '</td></tr>');
				items.push('<tr><td>Authors</td><td>' + data.authors.join(', ') + '</td></tr>');
				items.push('</table>');
				viewInfoContainer.html(items.join(''));
				
				var nodes = [];
				createTreeConfig(data.root, nodes);
				viewTreeContainer.empty();
				viewTreeContainer.dynatree({'minExpandLevel': 2, 'children': nodes, 'selectMode': 3, 'debugLevel': 0});
			});
		}
		
		function updateStudyDefinitionEditor() {
			// clear previous tree
			if (editTreeContainer.children('ul').length > 0)
				editTreeContainer.dynatree('destroy');
			editInfoContainer.empty();
			editTreeContainer.empty();
			editTreeContainer.html('Loading editor ...');
			
			// create new tree
			var studyDefinitionId = $('#studyDefinitionForm input[type="radio"]:checked').val();
			$.get('/plugin/studydefinitionmanager/edit/' + studyDefinitionId, function(data) {
				var items= [];
				items.push('<table class="table table-condensed table-borderless">');
				items.push('<tr><td>Version</td><td>' + data.version + '</td></tr>');
				items.push('<tr><td>Description</td><td>' + data.description + '</td></tr>');
				items.push('<tr><td>Authors</td><td>' + data.authors.join(', ') + '</td></tr>');
				items.push('</table>');
				editInfoContainer.html(items.join(''));
				
				var nodes = [];
				createTreeConfig2(data.root, nodes);console.log(nodes);
				editTreeContainer.empty();
				editTreeContainer.dynatree({'minExpandLevel': 2, 'children': nodes, 'selectMode': 3, 'debugLevel': 0, 'checkbox': true});
			});
		}
		
		$('#studyDefinitionForm input[type="radio"]').change(function() {
			if($('#study-definition-viewer').is(':visible'))
				updateStudyDefinitionViewer();
			if($('#study-definition-editor').is(':visible'))
				updateStudyDefinitionEditor();
		});
		$('a[data-toggle="tab"][href="#study-definition-viewer"]').on('show', function (e) {
			updateStudyDefinitionViewer();
		});
		$('a[data-toggle="tab"][href="#study-definition-editor"]').on('show', function (e) {
			updateStudyDefinitionEditor();
		});
		
		$('#studyDefinitionForm input[type="radio"]:checked').change();
	});
}($, window.top));