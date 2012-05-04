var explorerSelectedPath = null;

function continueModifyFromExplorer(action)
{
    var nodeName = $('#node-name').val().trim();
    if ( (action === "create") && (nodeName.length == 0) )
    {
        messageDialog("Error", "The node name is required.");
        return;
    }

    var userName = $('#node-data-user').val().trim();
    var ticketNumber = $('#node-data-ticket').val().trim();
    var reason = $('#node-data-reason').val().trim();
    if ( (userName.length == 0) || (ticketNumber.length == 0) || (reason.length == 0) )
    {
        messageDialog("Error", "The tracking fields are required.");
        return;
    }

    okCancelDialog("Are You Sure?", "Are you sure you want to do this. It cannot be undone and could irreparably harm the ZooKeeper data.", function(){
        var method = (action === "delete") ? "DELETE" : "PUT";
        var data = $('#node-data').val().trim();
        var localPath = explorerSelectedPath;
        if ( action === "create" )
        {
            if ( localPath != "/" )
            {
                localPath += "/";
            }
            localPath += nodeName;
        }

        $.ajax({
            type: method,
            url: URL_EXPLORER_ZNODE_BASE + localPath,
            data: data,
            contentType: 'application/json',
            headers: {
                'netflix-user-name': userName,
                'netflix-ticket-number': ticketNumber,
                'netflix-reason': reason
            },
            success:function(data)
            {
                if ( data.succeeded )
                {
                    $("#tree").dynatree("getTree").reload();
                    messageDialog("Success", "The change has been made.");
                }
                else
                {
                    messageDialog("Error", data.message);
                }
            }
        });
    });
}

function modifyFromExplorer(action)
{
    if ( (action != "create") && (explorerSelectedPath === "/") )
    {
        messageDialog("Error", "You cannot modify the root node.");
        return;
    }

    if ( action === "create" )
    {
        $('#node-name-container').show();
        $('#node-data-container').show();
    }
    else if ( action === "update" )
    {
        $('#node-name-container').hide();
        $('#node-data-container').show();
    }
    else    // delete
    {
        $('#node-name-container').hide();
        $('#node-data-container').hide();
    }

    $("#get-node-data-dialog").dialog("option", "buttons", {
            'Cancel': function (){
                $(this).dialog("close");
            },

            'Next...': function (){
                $(this).dialog("close");

                continueModifyFromExplorer(action);
            }
        }
    );
    $("#get-node-data-dialog").dialog("open");
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

                            $("#path").text(node.data.key);
                            $("#stat").text(data.stat);
                            $("#data-bytes").text(data.bytes);
                            $("#data-str").text(data.str);

                            $("#explorer-mutation-button-create").button("enable");
                            $("#explorer-mutation-button-set-data").button("enable");
                            $("#explorer-mutation-button-delete").button("enable");
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

    $("#explorer-mutation-button-create").button({
        icons:{
            primary:"ui-icon-document"
        },
        disabled: true
    }).click(function(){
        modifyFromExplorer("create");
        return false;
    });

    $("#explorer-mutation-button-set-data").button({
        icons:{
            primary:"ui-icon-pencil"
        },
        disabled: true
    }).click(function(){
        modifyFromExplorer("update");
        return false;
    });

    $("#explorer-mutation-button-delete").button({
        icons:{
            primary:"ui-icon-trash"
        },
        disabled: true
    }).click(function(){
        modifyFromExplorer("delete");
        return false;
    });

    $("#get-node-data-dialog").dialog({
        modal: true,
        autoOpen: false,
        width: 400,
        title: 'Modify Node'
    });
}
