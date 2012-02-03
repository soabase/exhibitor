var BUILTIN_TAB_QTY = 4;
var AUTO_REFRESH_PERIOD = 10000;
var UPDATE_STATE_PERIOD = 10000;

var doConfigUpdates = true;

function messageDialog(title, message)
{
    $('#message-dialog-text').html(message);
    $("#message-dialog").dialog("option", "title", title);
    $("#message-dialog").dialog("option", "buttons",
        {
            "OK":function ()
            {
                $(this).dialog("close");
            }
        }
    );
    $("#message-dialog").dialog("open");
}

function okCancelDialog(title, message, okFunction)
{
    $('#message-dialog-text').html(message);
    $("#message-dialog").dialog("option", "title", title);
    $("#message-dialog").dialog("option", "buttons",
        {
            "Cancel":function ()
            {
                $(this).dialog("close");
            },

            "OK":function ()
            {
                $(this).dialog("close");
                okFunction();
            }
        }
    );
    $("#message-dialog").dialog("open");
}

var systemState = {};
var systemConfig = {};
var connectedToExhibitor = true;
function updateState()
{
    $.getJSON('state',
        function (data)
        {
            systemState = data;
            if ( doConfigUpdates ) {
                systemConfig = systemState.config;
            }
            systemState.running = (
                data.running === "true"
                );    // it comes down as a string

            if ( !connectedToExhibitor )
            {
                connectedToExhibitor = true;
                messageDialog("", "Connection with the " + $('#app-name').html() + " server re-established.");
            }

            if ( systemState.running )
            {
                $('#tabs-main-running').show();
                $('#tabs-main-not-running').hide();
            }
            else
            {
                $('#tabs-main-running').hide();
                $('#tabs-main-not-running').show();
            }

            $('#version').html(systemState.version);
            $('#not-connected-alert').hide();
            $('#instance-hostname').html(systemConfig.thisHostname);
            $('#instance-id').html((
                systemConfig.thisServerId > 0
                ) ? systemConfig.thisServerId : "n/a");

            updateConfig();
        }).error(function ()
        {
            if ( connectedToExhibitor )
            {
                $('#not-connected-alert').show();
                connectedToExhibitor = false;
                messageDialog("Error", "The browser lost connection with the " + $('#app-name').html() + " server.");
            }
        });
}
updateState();

function submitConfigChanges()
{
    var newConfig = {};
    newConfig.zooKeeperInstallDir = $('#config-zookeeper-install-dir').val();
    newConfig.zooKeeperDataDir = $('#config-zookeeper-data-dir').val();
    newConfig.thisHostname = $('#config-hostname').val();
    newConfig.serversSpec = $('#config-servers-spec').val();
    newConfig.clientPort = $('#config-client-port').val();
    newConfig.connectPort = $('#config-connect-port').val();
    newConfig.electionPort = $('#config-election-port').val();
    newConfig.checkMs = $('#config-check-ms').val();
    newConfig.cleanupPeriodMs = $('#config-cleanup-ms').val();
    newConfig.cleanupMaxFiles = $('#config-cleanup-max-files').val();

    newConfig.connectionTimeoutMs = systemConfig.connectionTimeoutMs;
    newConfig.thisServerId = systemConfig.thisServerId;

    systemConfig = newConfig;

    var payload = JSON.stringify(newConfig);
    $.ajax({
        type: 'POST',
        url: 'set/config',
        data: payload,
        contentType: 'application/json'
    });
}

function ableConfig(enable)
{
    $('#config-zookeeper-install-dir').prop('disabled', !enable);
    $('#config-zookeeper-data-dir').prop('disabled', !enable);
    $('#config-hostname').prop('disabled', !enable);
    $('#config-servers-spec').prop('disabled', !enable);
    $('#config-client-port').prop('disabled', !enable);
    $('#config-connect-port').prop('disabled', !enable);
    $('#config-election-port').prop('disabled', !enable);
    $('#config-check-ms').prop('disabled', !enable);
    $('#config-cleanup-ms').prop('disabled', !enable);
    $('#config-cleanup-max-files').prop('disabled', !enable);

    $("#config-button").button(enable ? "enable" : "disable");
}

function updateConfig()
{
    if ( !doConfigUpdates ) {
        return;
    }

    $('#config-zookeeper-install-dir').val(systemConfig.zooKeeperInstallDir);
    $('#config-zookeeper-data-dir').val(systemConfig.zooKeeperDataDir);
    $('#config-hostname').val(systemConfig.thisHostname);
    $('#config-servers-spec').val(systemConfig.serversSpec);
    $('#config-client-port').val(systemConfig.clientPort);
    $('#config-connect-port').val(systemConfig.connectPort);
    $('#config-election-port').val(systemConfig.electionPort);
    $('#config-check-ms').val(systemConfig.checkMs);
    $('#config-cleanup-ms').val(systemConfig.cleanupPeriodMs);
    $('#config-cleanup-max-files').val(systemConfig.cleanupMaxFiles);
}

function refreshCurrentTab()
{
    var selected = $("#tabs").tabs("option", "selected");
    $("#tabs").tabs("load", selected);
}

function initExplorer()
{
    $("#tree").dynatree({
        onActivate:function (node)
        {
            $.ajax
                (
                    {
                        url:"node-data",
                        data:{"key":node.data.key},
                        cache:false,
                        dataType:'json',
                        success:function (data)
                        {
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
                        url:"node",
                        data:{"key":node.data.key},
                        cache:false
                    }
                );
        },

        onClick:function (node, event)
        {
            if ( node.getEventTargetType(event) == "expander" )
            {
                node.reloadChildren(function (node, isOk)
                {
                });
            }
            return true;
        },

        persist:false
    });
}

var refreshInterval = null;

function startAutoRefresh()
{
    if ( !refreshInterval )
    {
        refreshInterval = window.setInterval("refreshCurrentTab()", AUTO_REFRESH_PERIOD);
    }
    $.cookie("refresh-state-auto", "1");
}

function stopAutoRefresh()
{
    if ( refreshInterval )
    {
        window.clearInterval(refreshInterval);
        refreshInterval = null;
    }
    $.cookie("refresh-state-auto", "0");
}

$(function ()
{
    $.getJSON('tabs', function (data)
    {
        var uiTabSpec = $.makeArray(data.uiTabSpec);
        for ( var i = 0; i < uiTabSpec.length; ++i )
        {
            $('#tabs-list').append('<li><a href="' + uiTabSpec[i].url + '">' + uiTabSpec[i].name + '</a></li>');
        }
        $('#tabs').tabs({
            panelTemplate:'<div class="text"></div>',

            show:function (event, ui)
            {
                var selected = $("#tabs").tabs("option", "selected");
                if ( selected < BUILTIN_TAB_QTY )
                {
                    $("#refresh-state-container").hide();
                }
                else
                {
                    $("#refresh-state-container").show();
                }
            },

            create:function (event, ui)
            {
                initExplorer();
            }
        });
    });

    $("#stop-button").button({
        icons:{
            primary:"ui-icon-alert"
        }
    }).click(function ()
        {
            okCancelDialog("Restart ZooKeeper", "Are you sure you want to restart ZooKeeper?", function ()
            {
                $.get("stop");
                messageDialog("Restart ZooKeeper", "Stop request sent. Check the log for details.");
            });
            return false;
        });

    $("#start-button").button().click(function ()
    {
        var selected = $("#tabs").tabs("option", "selected");
        $("#tabs").tabs("load", selected);
        return false;
    });

    $("#message-dialog").dialog({
        modal:true,
        autoOpen:false
    });

    $('#instance-restarts-enabled').lightSwitch({
        switchImgCover: 'lightSwitch/switchplate.png',
        switchImg : 'lightSwitch/switch.png',
        disabledImg : 'lightSwitch/disabled.png'
    });

    $('#cleanup-enabled').lightSwitch({
        switchImgCover: 'lightSwitch/switchplate.png',
        switchImg : 'lightSwitch/switch.png',
        disabledImg : 'lightSwitch/disabled.png'
    });

    $('#config-editable').lightSwitch({
        switchImgCover: 'lightSwitch/switchplate.png',
        switchImg : 'lightSwitch/switch.png',
        disabledImg : 'lightSwitch/disabled.png'
    });
    $('#config-editable').next('span.switch').click(function(){
        var isChecked = $('#config-editable').is(':checked');
        doConfigUpdates = !isChecked;

        ableConfig(isChecked);
        if ( !isChecked ) {
            updateConfig();
        }
    });

    $("#config-button").button().click(function ()
    {
        return false;
    }).click(function(){
            okCancelDialog("Update", "Are you sure? If you've changed the Hostname or Servers it can cause instance restarts.", function() {
                submitConfigChanges();
            });
            return false;
        });

    if ( $.cookie("refresh-state-auto") )
    {
        startAutoRefresh();
        $('#refresh-state-checkbox').prop("checked", true);
    }
    $("#refresh-state-checkbox").lightSwitch({
        switchImgCover: 'lightSwitch/switchplate.png',
        switchImg : 'lightSwitch/switch.png',
        disabledImg : 'lightSwitch/disabled.png'
    });
    $('#refresh-state-checkbox').next('span.switch').click(function(){
        var isChecked = $('#refresh-state-checkbox').is(':checked');
        if ( isChecked )
        {
            startAutoRefresh();
        }
        else
        {
            stopAutoRefresh();
        }
    });

    $('#not-connected-message').html("Not connected to " + $('#app-name').html() + " server");
    $('#page-title').html($('#app-name').html() + " for ZooKeeper");

    $('#4ltr-button').button().click(function ()
    {
        var word = $('#4ltr-word').val();
        $('#4ltr-content').load('4ltr/' + encodeURIComponent(word));
        return false;
    });

    window.setInterval("updateState()", UPDATE_STATE_PERIOD);
    updateState();
    ableConfig(false);

    $('#config-group').colorTip();
});
