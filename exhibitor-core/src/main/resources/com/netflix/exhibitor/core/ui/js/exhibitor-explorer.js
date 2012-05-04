var explorerSelectedPath = null;

function continueModifyFromExplorer(action, n)
{
    if ( n == 0 )
    {
        okCancelDialog("Are You Sure?", "Are you sure you want to do this. It cannot be undone and could irreparably harm the ZooKeeper data.", function(){
            continueModifyFromExplorer(action, 1)
        });
    }
    else
    {
        okCancelDialog("Are You Really Sure?", 'Please confirm one more time that you want to make this modification to "' + explorerSelectedPath + '"', function(){
            var method = (action === "delete") ? "DELETE" : "PUT";
            var data = $('#node-data').val().trim();
            var localPath = explorerSelectedPath;
            if ( action === "create" )
            {
                localPath += "/" + $('#node-name').val().trim();
            }

            $.ajax({
                type: method,
                url: URL_EXPLORER_ZNODE_BASE + localPath,
                data: data,
                contentType: 'application/json',
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
}

function modifyFromExplorer(action)
{
    if ( action != "delete" )
    {
        if ( action === "create" )
        {
            $('#node-name-container').show();
        }
        else
        {
            $('#node-name-container').hide();
        }

        $("#get-node-data-dialog").dialog("option", "buttons", {
                'Cancel': function (){
                    $(this).dialog("close");
                },

                'OK': function (){
                    $(this).dialog("close");

                    if ( (action === "create") && ($('#node-name').val().trim().length == 0) )
                    {
                        return;
                    }

                    continueModifyFromExplorer(action, 0);
                }
            }
        );
        $("#get-node-data-dialog").dialog("open");
    }
    else
    {
        continueModifyFromExplorer(action, 0);
    }
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
