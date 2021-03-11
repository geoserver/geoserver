CheckAncestorsAndChildren = {

checkChildren : function(elementId, nodeChecked) {
	$('#' + elementId).closest('.tree-node').siblings('.tree-subtree').find('input[type=checkbox]').prop('checked', nodeChecked);
	if (!nodeChecked) {
		disabledVal = 'disabled';
		$('#' + elementId).closest('.tree-node').siblings('.tree-subtree').find('input[type=checkbox]').prop('disabled', disabledVal);
	} else {
	    $('#' + elementId).closest('.tree-node').siblings('.tree-subtree').find('input[type=checkbox]').removeProp('disabled');
	}
},

checkAncestors : function(elementId, nodeChecked){
	$('#' + elementId).parents('.tree-branch').each( function(){
			var curCheck = $(this).children('.tree-node').find('input[type=checkbox]');
			if(nodeChecked){
				curCheck.prop('checked', nodeChecked)
				return;
			}
			var checkedChildren = $(this).children('.tree-subtree').find(':checked').size();
			if(checkedChildren == 0)
				curCheck.prop('checked', nodeChecked)
		});
	}

};