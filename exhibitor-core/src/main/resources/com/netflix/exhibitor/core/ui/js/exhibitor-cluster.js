var baseServerItem = null;
var currentServersSpec = null;
var currentHostname = null;
var serverItemsVersion = 0;

var STATE_UNKNOWN = 0;
var STATE_LATENT = 1;
var STATE_NOT_SERVING = 2;
var STATE_SERVING = 3;
var STATE_DOWN_BECAUSE_UNLISTED = 4;
var STATE_DOWN_BECAUSE_RESTARTS_TURNED_OFF = 5;

function makeServersList()
{
    var serverList = new Array();

    var specs = systemConfig.serversSpec.split(",");
    var foundUs = false;
    for ( var i = 0; i < specs.length; ++i )
    {
        var parts = specs[i].split(":");

        var thisSpec = {};
        thisSpec.serverId = parseInt(parts[0]);
        thisSpec.hostname = parts[1];
        if ( thisSpec.hostname === systemConfig.hostname )
        {
            foundUs = true;
        }

        serverList.push(thisSpec);
    }
    if ( !foundUs )
    {
        thisSpec = {};
        thisSpec.serverId = -1;
        thisSpec.hostname = systemConfig.hostname;
        serverList.push(thisSpec);
    }

    serverList.sort(function(a, b){
        return a.serverId - b.serverId;
    });

    return serverList;
}

function buildServerItems()
{
    if ( baseServerItem == null )
    {
        baseServerItem = $('#base-server-item').html();
    }

    var serversList = makeServersList();
    if ( (currentServersSpec != systemConfig.serversSpec) || (currentHostname != systemConfig.hostname) )
    {
        ++serverItemsVersion;
        internalBuildServerItems(serversList);
    }
    updateServerState(serversList);
}

function internalBuildServerItems(serversList)
{
    currentServersSpec = systemConfig.serversSpec;
    currentHostname = systemConfig.hostname;

    var content = "";
    for ( var i = 0; i < serversList.length; ++i )
    {
        var spec = serversList[i];
        var domId = 'cp-' + i;

        var thisItem = baseServerItem;
        thisItem = thisItem.replace("$SERVER_ID$", spec.serverId);
        thisItem = thisItem.replace("$SERVER_HOSTNAME$", spec.hostname);
        thisItem = thisItem.replace(/\$BASE_ID\$/gi, domId);

        content += thisItem;
    }
    $('#base-server-item').html(content);

    for ( i = 0; i < serversList.length; ++i )
    {
        spec = serversList[i];
        domId = '#cp-' + i;

        makeLightSwitch(domId + '-instance-restarts-enabled', function(){}, true);
        makeLightSwitch(domId + '-cleanup-enabled', function(){}, true);
        makeLightSwitch(domId + '-unlisted-restarts', function(){}, true);
        makeLightSwitch(domId + '-backups-enabled', function(){}, true);
        if ( systemState.backupActive )
        {
            $(domId + '-backups-enabled-control').show();
        }

        var serverIdContent = "Server Id: " + spec.serverId;
        var hostnameContent = "Hostname: " + spec.hostname;
        if ( spec.hostname === systemConfig.hostname )
        {
            hostnameContent += '<span class="cp-this-server">(This server)</span>';
        }

        $(domId + '-server-id').html(serverIdContent);
        $(domId + '-hostname').html(hostnameContent);

        $(domId + '-power-button').button({
            disabled: true,
            icons:{
                primary:"ui-icon-play"
            }
        }).click(function ()
            {
                return false;
            });

        $(domId + '-4ltr-button').button({
            disabled: true,
            icons:{
                primary:"ui-icon-circle-zoomin"
            }
        }).click(function ()
            {
                return false;
            });
    }

    $('#base-server-item').colorTip();
}

function updateOneServerState(index, data)
{
    var statusColor;
    var statusMessage = "";

    domId = '#cp-' + index;
    if ( data.success )
    {
        switch ( data.response.state )
        {
            default:
            case STATE_UNKNOWN:
            case STATE_LATENT:
            {
                statusColor = "#FFF";
                break;
            }

            case STATE_SERVING:
            {
                statusColor = "#0E0";
                break;
            }

            case STATE_DOWN_BECAUSE_UNLISTED:
            case STATE_DOWN_BECAUSE_RESTARTS_TURNED_OFF:
            case STATE_NOT_SERVING:
            {
                statusColor = "#F00";
                break;
            }
        }
    }
    else
    {
        statusColor = "#F00";
        statusMessage = data.errorMessage;
    }
    $(domId + '-status-indicator').css('background-color', statusColor);
    $(domId + '-status-message').html(statusMessage);
}

function updateServerState(serversList)
{
    var localServerItemsVersion = serverItemsVersion;
    for ( var i = 0; i < serversList.length; ++i )
    {
        var spec = serversList[i];
        var thisHostname = spec.hostname;
        if ( thisHostname === systemConfig.hostname )
        {
            thisHostname = "localhost";
        }
        var callback = function(index) {
            return function(data) {
                if ( serverItemsVersion === localServerItemsVersion )
                {
                    updateOneServerState(index, data);
                }
            };
        };
        $.getJSON('cluster/state/' + thisHostname, callback(i));
    }
}
