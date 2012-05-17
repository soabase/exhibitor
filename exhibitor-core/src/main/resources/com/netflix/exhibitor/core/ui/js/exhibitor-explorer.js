var explorerSelectedPath = null;
var explorerSelectedBytes = null;

var analyzePathsContainerContent = null;
var analyzePathsContainerQty = 0;

function removeFromAnalyzePathsContainer()
{
    if ( analyzePathsContainerQty > 1 )
    {
        --analyzePathsContainerQty;
        $('#analyze-paths-' + analyzePathsContainerQty).remove();
    }
}

function addToAnalyzePathsContainer()
{
    var requestData = buildRequestData();

    var     newItem = new String(analyzePathsContainerContent);
    newItem = newItem.replace(/\$INDEX\$/g, analyzePathsContainerQty);
    newItem = newItem.replace(/\$NUMBER\$/g, analyzePathsContainerQty + 1);

    var currentContent = $('#analyze-paths-container').html();
    $('#analyze-paths-container').html(currentContent + newItem);

    for ( var i = 0; i < requestData.length; ++i )
    {
        $('#analyze-node-name-' + i).val(requestData[i].path);
        $('#analyze-max-' + i).val(requestData[i].max);
    }

    ++analyzePathsContainerQty;
}

function showAnalysisResults(data)
{
    var details = "";
    var i;
    var j;

    details += "<i>Possible Deadlocks:</i><br><br>";
    if ( !data.possibleCycles || (data.possibleCycles.length == 0) )
    {
        details += "None<br>";
    }
    else
    {
        for ( i = 0; i < data.possibleCycles.length; ++i )

        {
            var thisIds = data.possibleCycles[i].ids;
            for ( j = 0; j < thisIds.length; ++j )
            {
                if ( j > 0 )
                {
                    details += ", ";
                }
                details += thisIds[j];
            }
            details += "<br>";
        }
    }
    details += "<br><hr><br>";

    details += "<i>Locks/Owners:</i><br><br>";
    for ( i = 0; i < data.nodes.length; ++i )
    {
        var thisNode = data.nodes[i];

        details += "Lock Path: " + thisNode.path + "<br>";
        details += "Owner(s): ";
        for ( j = 0; (j < thisNode.max) && (j < thisNode.childIds.length); ++j )
        {
            if ( j > 0 )
            {
                details += ", ";
            }
            details += thisNode.childIds[j];
        }
        details += "<br><br>";
    }

    $('#analyze-results').html(details);
    $('#analyze-results-dialog').dialog("open");
}

function buildRequestData()
{
    var requestTab = new Array();
    for ( var i = 0; i < analyzePathsContainerQty; ++i )
    {
        var requestItem = {};
        requestItem.path = $('#analyze-node-name-' + i).val();
        requestItem.max = parseInt($('#analyze-max-' + i).val());
        requestTab.push(requestItem);
    }
    return requestTab;
}
function performAnalyze()
{
    var requestTab = buildRequestData();
    var json = JSON.stringify(requestTab);
    $.ajax({
        url: URL_EXPLORER_ANALYZE,
        type: 'POST',
        data: json,
        cache: false,
        contentType: 'application/json',
        dataType: 'json',
        success:function (data){
            if ( data.error.length > 0 )
            {
                messageDialog("Error", data.error);
            }
            else
            {
                showAnalysisResults(data);
            }
        }
    });
}

function openAnalyzeDialog(path)
{
    if ( !analyzePathsContainerContent )
    {
        analyzePathsContainerContent = $('#analyze-paths-container').html();
        $('#analyze-paths-container').html("");
        addToAnalyzePathsContainer();
    }

    $('#analyze-node-name-0').val(path);

    $("#analyze-dialog").dialog("option", "buttons", {
            "Cancel": function(){
                $(this).dialog("close");
            },

            "OK": function(){
                $(this).dialog("close");
                performAnalyze();
            }
        }
    );
    $("#analyze-dialog").dialog("open");
}

function initExplorer()
{
    $("#tree").dynatree({
        onActivate:function (node)
        {
            $.ajax
                (
                    {
                        url: URL_EXPLORER_NODE_DATA,
                        data: {"key":node.data.key},
                        cache: false,
                        dataType: 'json',
                        success:function (data){
                            explorerSelectedPath = node.data.key;
                            explorerSelectedBytes = data.bytes;

                            $("#path").text(node.data.key);
                            $("#stat").text(data.stat);
                            $("#data-bytes").text(data.bytes);
                            $("#data-str").text(data.str);
                        }
                    }
                );
        },

        selectMode:1,

        children:[
            {title:"/", isFolder:true, isLazy:true, key:"/", expand:false, noLink:true}
        ],

        onLazyRead:function (node)
        {
            node.appendAjax
                (
                    {
                        url: URL_EXPLORER_NODE,
                        data:{"key":node.data.key},
                        cache:false
                    }
                );
        },

        onClick:function (node, event)
        {
            if ( node.getEventTargetType(event) == "expander" )
            {
                node.reloadChildren(function (node, isOk){
                });
            }
            return true;
        },

        persist:false
    });

    $("#explorer-button-modify").button({
        icons:{
            primary:"ui-icon-pencil"
        }
    }).click(function(){
        var localData = explorerSelectedBytes;
        var localDataType;
        if ( !localData || !localData.length )
        {
            localData = "";
            localDataType = "string";
        }
        else
        {
            localDataType = "binary";
        }

        openModifyDialog("update", explorerSelectedPath, localData, localDataType);
        return false;
    });

    $("#explorer-button-analyze").button({
        icons:{
            primary:"ui-icon-search"
        }
    }).click(function(){
        openAnalyzeDialog(explorerSelectedPath);
        return false;
    });

    $("#analyze-dialog").dialog({
        modal: true,
        autoOpen: false,
        width: 500,
        resizable: false,
        title: 'Analyze'
    });

    $("#analyze-results-dialog").dialog({
        modal: true,
        autoOpen: false,
        width: 500,
        resizable: true,
        title: 'Analysis',
        buttons: {
            "OK": function(){
                $(this).dialog("close");
            }
        }
    });

    $("#analyze-plus").button({
        text: false,
        icons:{
            primary:"ui-icon-plus"
        }
    }).click(function(){
        addToAnalyzePathsContainer();
        return false;
    });

    $("#analyze-minus").button({
        text: false,
        icons:{
            primary:"ui-icon-minus"
        }
    }).click(function(){
        removeFromAnalyzePathsContainer();
        return false;
    });
}

