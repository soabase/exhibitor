var BUILTIN_TAB_QTY = 5;
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

var hasBackupConfig = false;
var systemState = {};
var systemConfig = {};
var connectedToExhibitor = true;
function updateState()
{
    if ( !hasBackupConfig )
    {
        $.getJSON('backup-config', function(data){
            hasBackupConfig = true;
            addBackupExtraConfig(data);
        });
        return;
    }

    $.getJSON('state',
        function (data)
        {
            systemState = data;
            if ( doConfigUpdates ) {
                systemConfig = systemState.config;
            }

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

            if ( systemState.backupActive )
            {
                $('#config-backups-fieldset').show();
                $('#backups-enabled-control').show();
            }
            else
            {
                $('#config-backups-fieldset').hide();
                $('#backups-enabled-control').hide();
            }

            $('#instance-restarts-enabled').prop("checked", systemState.restartsEnabled);
            $('#instance-restarts-enabled').trigger("change");

            $('#unlisted-restarts').prop("checked", systemState.unlistedRestartsEnabled);
            $('#unlisted-restarts').trigger("change");

            $('#cleanup-enabled').prop("checked", systemState.cleanupEnabled);
            $('#cleanup-enabled').trigger("change");

            $('#backups-enabled').prop("checked", systemState.backupsEnabled);
            $('#backups-enabled').trigger("change");

            $('#exhibitor-valence').hide();
            $('#version').html(systemState.version);
            $('#not-connected-alert').hide();
            $('#instance-hostname').html(systemConfig.hostname);
            $('#instance-id').html((
                systemConfig.serverId > 0
                ) ? systemConfig.serverId : "n/a");

            updateConfig();
        }).error(function ()
        {
            if ( connectedToExhibitor )
            {
                $('#exhibitor-valence').height($(document).height());
                $('#exhibitor-valence').show();
                $('#not-connected-alert').show();
                connectedToExhibitor = false;
                messageDialog("Error", "The browser lost connection with the " + $('#app-name').html() + " server.");
            }
        });
}
updateState();

var configExtraTab = new Array();

function addBackupExtraConfig(data)
{
    configExtraTab = $.makeArray(data);
    var extra = "";
    for ( var i = 0; i < configExtraTab.length; ++i )
    {
        var c = configExtraTab[i];
        var id = getBackupExtraId(c);
        var fieldSize = (c.type === "s") ? 60 : 5;
        extra += '<label for="' + id + '">' + c.name + '</label><input type="text" id="' + id + '" name="' + id + '" size="' + fieldSize + '" title="' + c.help + '"><br clear="all"/>';
    }

    $('#config-backups-extra').html(extra);
    for ( i = 0; i < configExtraTab.length; ++i )
    {
        c = configExtraTab[i];
        if ( c.type === "i" )
        {
            id = getBackupExtraId(c);
            $('#' + id).keyfilter($.fn.keyfilter.defaults.masks['pint']);
        }
    }

    updateState();

    var isChecked = $('#config-editable').is(':checked');
    ableConfig(isChecked);
}

function submitConfigChanges()
{
    var newConfig = {};
    newConfig.zookeeperInstallDirectory = $('#config-zookeeper-install-dir').val();
    newConfig.zookeeperDataDirectory = $('#config-zookeeper-data-dir').val();
    newConfig.logIndexDirectory = $('#config-log-index-dir').val();
    newConfig.serversSpec = $('#config-servers-spec').val();
    newConfig.clientPort = $('#config-client-port').val();
    newConfig.connectPort = $('#config-connect-port').val();
    newConfig.electionPort = $('#config-election-port').val();
    newConfig.checkMs = $('#config-check-ms').val();
    newConfig.cleanupPeriodMs = $('#config-cleanup-ms').val();
    newConfig.cleanupMaxFiles = $('#config-cleanup-max-files').val();
    newConfig.backupPeriodMs = $('#config-backup-ms').val();
    newConfig.backupMaxFiles = $('#config-backup-max-files').val();

    newConfig.backupExtra = {};
    for ( var i = 0; i < configExtraTab.length; ++i )
    {
        var c = configExtraTab[i];
        var id = getBackupExtraId(c);
        newConfig.backupExtra[c.key] = $('#' + id).val();
    }

    newConfig.connectionTimeoutMs = systemConfig.connectionTimeoutMs;
    newConfig.serverId = systemConfig.serverId;

    systemConfig = newConfig;

    var payload = JSON.stringify(newConfig);
    $.ajax({
        type: 'POST',
        url: 'set/config',
        data: payload,
        contentType: 'application/json',
        success:function(data)
        {
            if ( data.succeeded != "true" )
            {
                messageDialog("Error", data.message);
            }
        }
    });
}

function getBackupExtraId(obj)
{
    return 'config-backup-extra-' + obj.key;
}

function ableConfig(enable)
{
    $('#config-zookeeper-install-dir').prop('disabled', !enable);
    $('#config-zookeeper-data-dir').prop('disabled', !enable);
    $('#config-log-index-dir').prop('disabled', !enable);
    $('#config-servers-spec').prop('disabled', !enable);
    $('#config-client-port').prop('disabled', !enable);
    $('#config-connect-port').prop('disabled', !enable);
    $('#config-election-port').prop('disabled', !enable);
    $('#config-check-ms').prop('disabled', !enable);
    $('#config-cleanup-ms').prop('disabled', !enable);
    $('#config-cleanup-max-files').prop('disabled', !enable);
    $('#config-backup-ms').prop('disabled', !enable);
    $('#config-backup-max-files').prop('disabled', !enable);

    for ( var i = 0; i < configExtraTab.length; ++i )
    {
        var c = configExtraTab[i];
        var id = getBackupExtraId(c);
        $('#' + id).prop('disabled', !enable);
    }

    $("#config-button").button(enable ? "enable" : "disable");
}

function updateConfig()
{
    if ( !doConfigUpdates ) {
        return;
    }

    $('#config-zookeeper-install-dir').val(systemConfig.zookeeperInstallDirectory);
    $('#config-zookeeper-data-dir').val(systemConfig.zookeeperDataDirectory);
    $('#config-log-index-dir').val(systemConfig.logIndexDirectory);
    $('#config-servers-spec').val(systemConfig.serversSpec);
    $('#config-client-port').val(systemConfig.clientPort);
    $('#config-connect-port').val(systemConfig.connectPort);
    $('#config-election-port').val(systemConfig.electionPort);
    $('#config-check-ms').val(systemConfig.checkMs);
    $('#config-cleanup-ms').val(systemConfig.cleanupPeriodMs);
    $('#config-cleanup-max-files').val(systemConfig.cleanupMaxFiles);
    $('#config-backup-ms').val(systemConfig.backupPeriodMs);
    $('#config-backup-max-files').val(systemConfig.backupMaxFiles);

    for ( i = 0; i < configExtraTab.length; ++i )
    {
        c = configExtraTab[i];
        id = getBackupExtraId(c);
        $('#' + id).val(systemConfig.backupExtra[c.key]);
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
                        url:"explorer/node-data",
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
                        url:"explorer/node",
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

function refreshCurrentTab()
{
    var selected = $("#tabs").tabs("option", "selected");
    if ( selected == 3 )
    {
        var radio = $('input:radio:checked[name="restore-item-radio"]');
        updateRestoreItems(radio.val());
    }
    else if ( selected >= BUILTIN_TAB_QTY )
    {
        var index = selected - BUILTIN_TAB_QTY;
        $("#" + customTabs[index].contentId).load(customTabs[index].url);
    }
}

function updateCalculatorValue()
{
    var value = parseInt($('#millisecond-calculator-value').val());
    var unit = parseInt($('#millisecond-calculator-unit').val());
    $('#millisecond-calculator-result').html(value * unit);
}

var customTabs = new Array();
$(function ()
{
    $.getJSON('tabs', function (data)
    {
        var uiTabSpec = $.makeArray(data.uiTabSpec);
        for ( var i = 0; i < uiTabSpec.length; ++i )
        {
            var tabData = {};
            tabData.id = 'tabs-custom-' + i;
            tabData.contentId = 'tabs-custom-content' + i;
            tabData.url = uiTabSpec[i].url;
            customTabs[i] = tabData;

            $('#tabs').append('<div id="' + tabData.id + '" class="ui-helper-hidden"><div id="' + tabData.contentId + '" class="text"></div></div>')
            $('#tabs-list').append('<li><a href="#' + tabData.id + '">' + uiTabSpec[i].name + '</a></li>');
        }
        $('#tabs').tabs({
            panelTemplate:'<div><div class="text"></div></div>',

            show:function (event, ui)
            {
                refreshCurrentTab();
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
        modal: true,
        autoOpen: false,
        zIndex: 99999
    });

    $('#instance-restarts-enabled').lightSwitch({
        switchImgCover: 'lightSwitch/switchplate.png',
        switchImg : 'lightSwitch/switch.png',
        disabledImg : 'lightSwitch/disabled.png'
    });
    $('#instance-restarts-enabled').next('span.switch').click(function(){
        var isChecked = $('#instance-restarts-enabled').is(':checked');
        $.get("set/restarts/" + (isChecked ? "true" : "false"));
    });

    $('#cleanup-enabled').lightSwitch({
        switchImgCover: 'lightSwitch/switchplate.png',
        switchImg : 'lightSwitch/switch.png',
        disabledImg : 'lightSwitch/disabled.png'
    });
    $('#cleanup-enabled').next('span.switch').click(function(){
        var isChecked = $('#cleanup-enabled').is(':checked');
        $.get("set/cleanup/" + (isChecked ? "true" : "false"));
    });

    $('#unlisted-restarts').lightSwitch({
        switchImgCover: 'lightSwitch/switchplate.png',
        switchImg : 'lightSwitch/switch.png',
        disabledImg : 'lightSwitch/disabled.png'
    });
    $('#unlisted-restarts').next('span.switch').click(function(){
        var isChecked = $('#unlisted-restarts').is(':checked');
        $.get("set/unlisted-restarts/" + (isChecked ? "true" : "false"));
    });

    $('#config-editable').lightSwitch({
        switchImgCover: 'lightSwitch/switchplate.png',
        switchImg : 'lightSwitch/switch.png',
        disabledImg : 'lightSwitch/disabled.png'
    });
    $('#config-editable').next('span.switch').click(function(){
        var isChecked = $('#config-editable').is(':checked');
        doConfigUpdates = !isChecked;

        if ( !isChecked ) {
            updateConfig();
        }
        ableConfig(isChecked);
    });

    $("#config-button").button({
        icons:{
            primary:"ui-icon-disk"
        }
    }).click(function ()
    {
        return false;
    }).click(function(){
            okCancelDialog("Update", "Are you sure? If you've changed the Servers it can cause instance restarts.", function() {
                submitConfigChanges();
            });
            return false;
        });

    $('#calculator-button').button({
        icons:{
            primary:"ui-icon-calculator"
        }
    }).click(function(){
        updateCalculatorValue();
        $('#millisecond-calculator-dialog').dialog("open");
    });

    $('#not-connected-message').html("Not connected to " + $('#app-name').html() + " server");
    $('#page-title').html($('#app-name').html() + " for ZooKeeper");

    $('#4ltr-button').button().click(function ()
    {
        var word = $('#4ltr-word').val();
        $('#4ltr-content').load('4ltr/' + encodeURIComponent(word));
        return false;
    });

    $('#backups-enabled').lightSwitch({
        switchImgCover: 'lightSwitch/switchplate.png',
        switchImg : 'lightSwitch/switch.png',
        disabledImg : 'lightSwitch/disabled.png'
    });
    $('#backups-enabled').next('span.switch').click(function(){
        var isChecked = $('#backups-enabled').is(':checked');
        $.get("set/backups/" + (isChecked ? "true" : "false"));
    });

    window.setInterval("updateState()", UPDATE_STATE_PERIOD);
    updateState();
    ableConfig(false);

    $('#config-group').colorTip();
    $('#control-panel').colorTip();

    $("#millisecond-calculator-dialog").dialog({
        modal: true,
        title: 'Converter',
        autoOpen: false
    });
    $('#millisecond-calculator-value').keyup(function(){
        updateCalculatorValue();
    });
    $('#millisecond-calculator-unit').change(function(){
        updateCalculatorValue();
    });

    $.get('able-backups/true');
    initRestoreUI();
    window.setInterval("refreshCurrentTab()", AUTO_REFRESH_PERIOD);
});
