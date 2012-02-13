var ACTION_NAMES = [
    "Create-Persistent",
    "Create-Ephemeral",
    "Delete",
    "Set Data"
];

function initRestoreUI()
{
    $('#restore-open-button').button({
        icons:{
            primary:"ui-icon-folder-open"
        }
    }).click(function(){
            var radio = $('input:radio:checked[name="restore-item-radio"]');
            openIndex(radio.val());
            return false;
        });

    $('#restore-delete-button').button({
        icons:{
            primary:"ui-icon-trash"
        }
    }).click(function(){
            okCancelDialog("Delete Index", "Are you sure you to delete the selected index? This CANNOT be un-done.", function ()
            {
                var radio = $('input:radio:checked[name="restore-item-radio"]');
                $.ajax({
                    type: 'DELETE',
                    url: 'index/' + radio.val()
                });
                messageDialog('Index', 'Index is marked for deletion. Check the log for details.');
            });
            return false;
        });

    $('#restore-index-button').button({
        icons:{
            primary:"ui-icon-document"
        }
    }).click(function(){
            $('#new-index-dialog').dialog("open");
            return false;
        });

    $('#index-query-filter-button').button({
        icons:{
            primary:"ui-icon-search"
        }
    }).click(function(){
            $('#index-query-dialog').dialog("open");
            return false;
        });
    $('#index-query-clear-filter-button').button({
        icons:{
            primary: "ui-icon-close"
        },
        disabled: true
    }).click(function(){
            var indexName = $('#index-query-dialog').attr("indexName");
            openIndex(indexName);
            return false;
        });
    $('#index-query-clear-restore-button').button({
        icons:{
            primary: "ui-icon-alert"
        },
        disabled: true
    }).click(function(){
            openRestoreDialog();
            return false;
        });

    $('#index-query-dialog').dialog({
        modal: true,
        autoOpen: false,
        title: 'Search Index',
        minWidth: 400
    });
    $('#index-query-from').datepicker({
        dateFormat: "yy-mm-dd"
    });
    $('#index-query-to').datepicker({
        dateFormat: "yy-mm-dd"
    });
    $('#index-query-dialog').dialog("option", "buttons",
        {
            "Cancel":function ()
            {
                $(this).dialog("close");
            },

            "OK":function ()
            {
                var indexName = $('#index-query-dialog').attr("indexName");
                var indexHandle = $('#index-query-dialog').attr("indexHandle");
                var searchRequest = buildSearchRequestFromFilter(indexName, indexHandle);
                $(this).dialog("close");
                filterIndex(indexName, searchRequest, true);
            }
        }
    );

    $('#index-query-restore-dialog').dialog({
        modal: true,
        autoOpen: false,
        title: 'Restore',
        minWidth: 600
    });
    $('#index-query-restore-dialog').dialog("option", "buttons",
        {
            "Cancel":function ()
            {
                $(this).dialog("close");
            },

            "OK":function ()
            {
                $(this).dialog("close");
                submitRestore();
            }
        }
    );

    $('#new-index-dialog').dialog({
        modal: true,
        autoOpen: false,
        title: 'New Index',
        minWidth: 450
    });
    $('#new-index-dialog').dialog("option", "buttons",
        {
            "Cancel":function ()
            {
                $(this).dialog("close");
            },

            "OK":function ()
            {
                var newIndexRequest = {};
                newIndexRequest.path = $('#new-index-radio-default').prop("checked") ? null : $('#new-index-path').val();
                $(this).dialog("close");
                var payload = JSON.stringify(newIndexRequest);
                $.ajax({
                    type: 'POST',
                    url: 'index/new-index',
                    data: payload,
                    contentType: 'application/json'
                });
                messageDialog('Index', 'Indexing has started. Check the log for details.');
            }
        }
    );

    $('#new-index-path').change(function(){
        $('#new-index-radio-path').prop("checked", true);
    });
    $('#new-index-path').keydown(function(){
        $('#new-index-radio-path').prop("checked", true);
    });

    $('#index-query-results-dialog').dialog({
        modal: true,
        autoOpen: false,
        title: 'Results',
        minWidth: 800
    });
}

function submitRestore()
{
    var indexName = $('#index-query-dialog').attr("indexName");
    var indexHandle = $('#index-query-dialog').attr("indexHandle");
    $.getJSON('index/restore/' + indexName + "/" + indexHandle + "/" + selectedIndexData.docId, function(data){
        messageDialog('Restore', 'Restore request sent. Check the log for details.');
    });
}

function openRestoreDialog()
{
    $('#index-query-restore-action').html(ACTION_NAMES[selectedIndexData.type]);
    $('#index-query-restore-path').html(selectedIndexData.path);
    $('#index-query-restore-date').html(selectedIndexData.date);

    $('#index-query-restore-dialog').dialog("open");
}

function updateRestoreItems(selectedRadio)
{
    $.getJSON('index/indexed-logs', function(data){
        var itemsTab = data ? $.makeArray(data.index) : new Array();
        var items = '<table id="restore-items-table">';
        items += '<thead><tr><th></th><th>Name</th><th>From</th><th>To</th><th>Count</th></tr></thead><tbody>';

        if ( itemsTab.length == 0 )
        {
            items += '<tr><td></td><td></td><td></td><td></td><td></td></tr>';
        }

        var needsCheck = true;
        for ( var i = 0; i < itemsTab.length; ++i )
        {
            var item = itemsTab[i];
            items += '<tr>';
            items += '<td><input type="radio" id="restore-item-radio-' + i + '" name="restore-item-radio" value="' + item.name + '"';
            if ( selectedRadio )
            {
                if ( selectedRadio === item.name )
                {
                    items += " CHECKED";
                    needsCheck = false;
                }
            }
            items += '></td>';
            items += '<td>' + item.name + '</td>';
            items += '<td>' + item.from + '</td>';
            items += '<td>' + item.to + '</td>';
            items += '<td>' + item.entryCount + '</td>';
            items += '</tr>';
        }
        items += '</tbody></table>';

        $('#restore-items').html(items);
        $('#restore-items-table').dataTable({
            "bPaginate": false,
            "bLengthChange": false,
            "bSort": false,
            "bInfo": false,
            "bFilter": false,
            "bAutoWidth": false
        });

        if ( needsCheck && (itemsTab.length > 0) )
        {
            $('#restore-item-radio-0').prop("checked", true);
        }

        $("#restore-open-button").button((itemsTab.length > 0) ? "enable" : "disable");
        $("#restore-delete-button").button((itemsTab.length > 0) ? "enable" : "disable");
    });
}

var selectedIndexData = null;
function applySelectedValue(data)
{
    selectedIndexData = data;

    var localBytes = data.dataBytes;
    if ( localBytes.length === 0 )
    {
        localBytes = "[]";
    }
    $('#index-query-results-selected-path').html(data.path);
    $('#index-query-results-selected-date').html(data.date);
    $('#index-query-results-selected-data-bytes').html(localBytes);
    $('#index-query-results-selected-data-string').html(data.dataAsString);
}

function viewIndex(indexName, indexHandle, isFromFilter)
{
    $('#index-query-dialog').attr("indexName", indexName);
    $('#index-query-dialog').attr("indexHandle", indexHandle);

    var emptyData = {
        type: 0,
        date: "",
        path: "",
        dataBytes: "",
        dataAsString: ""
    };
    applySelectedValue(emptyData);
    var selectedRowId = -1;
    $('#index-query-results-table').dataTable({
        sAjaxSource: "index/dataTable/" + indexName + "/" + indexHandle,
        bDestroy: true,
        bProcessing: true,
        bServerSide: true,
        bStateSave: false,
        bFilter: false,
        bSort: false,
        sScrollY: "300px",
        sDom: "frtiS",
        bDeferRender: true,
        iDeferLoading: false,
        fnRowCallback: function( row, data, index ) {
            if ( data.DT_RowId === selectedRowId ) {
                $(row).addClass('row_selected');
            }
            return row;
        }
    });

    $('#index-query-results-table tbody tr').live('click', function () {
        var id = this.id;
        if ( id != selectedRowId )
        {
            if ( selectedRowId != -1 )
            {
                $('#' + selectedRowId).removeClass('row_selected');
            }

            selectedRowId = id;
            $(this).addClass('row_selected');

            var docId = selectedRowId.split('-').pop();
            $.getJSON('index/get/' + indexName + "/" + indexHandle + "/" + docId, applySelectedValue);
            $('#index-query-clear-restore-button').button("option", "disabled", false);
        }
    });

    $('#index-query-clear-filter-button').button("option", "disabled", !isFromFilter);
    $('#index-query-clear-restore-button').button("option", "disabled", true);

    $('#index-query-results-dialog').bind('dialogclose', function(event, ui) {
        $.get('index/release-cache/' + indexName + '/' + indexHandle);
    });

    $('#index-query-results-dialog').dialog("option", "title", 'Results for ' + indexName);
    $('#index-query-results-dialog').dialog("option", "maxHeight", 600);
    $('#index-query-results-dialog').dialog("open");
}

function buildSearchRequestFromFilter(indexName, indexHandle)
{
    var searchRequest = {};
    searchRequest.indexName = indexName;
    searchRequest.reuseHandle = indexHandle;
        searchRequest.pathPrefix = $('#index-query-path').val().trim();
    searchRequest.operationType = ($('#index-query-type').val() === "") ? null : $('#index-query-type').val();
    searchRequest.maxResults = $('#index-query-max').val();
    searchRequest.firstDate = $('#index-query-from').datepicker("getDate");
    searchRequest.secondDate = $('#index-query-to').datepicker("getDate");

    return searchRequest;
}

function filterIndex(indexName, searchRequest, isFromFilter)
{
    var payload = JSON.stringify(searchRequest);
    $.ajax({
        type: 'POST',
        url: 'index/cache-search',
        data: payload,
        contentType: 'application/json',
        success: function(data){
            viewIndex(indexName, data.id, isFromFilter);
        }
    });
}

function openIndex(indexName)
{
    var searchRequest = {};
    searchRequest.indexName = indexName;
    searchRequest.pathPrefix = "";
    searchRequest.operationType = -1;
    searchRequest.reuseHandle = null;
    searchRequest.maxResults = 0;

    filterIndex(indexName, searchRequest, false);
}
