var explorerSelectedPath = null;

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

    $("#explorer-mutation-button-modify").button({
        icons:{
            primary:"ui-icon-pencil"
        }
    }).click(function(){
        openModifyDialog("update", explorerSelectedPath, "");
        return false;
    });
}
