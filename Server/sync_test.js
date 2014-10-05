var express = require('express');
var http = require('http');
var url = require('url');
var fs = require('fs');
var path = require('path');
var socketIO = require('socket.io');

var CLIENT_PLAY_FUTURE_DELAY = 2000;
var CLIENT_PLAY_POSITION_OFFSET = -500;
var CLIENT_PLAY_RETRY_OFFSET = 5000;

var app = express();
var server = http.createServer(app);
var io =  socketIO.listen(server);

var clientCount = 0;
var clientIDs = {};

var isPlaying = false;
var currentSongStartTime = 0;
var currentSongPauseTime = 0;
var currentSongDuration = 0;

function getCurrentSongPosition() {
    var position = 0;
    if(isPlaying) {
        position = getServerTime() - currentSongStartTime;
    } else {
        position = currentSongPauseTime - currentSongStartTime;
    }
    if(position > 0) {
        return position;
    } else {
        return 0;
    }
}

function getServerTime() {
    var time = process.hrtime();
    return (time[0] * 1000) + (Math.round(time[1] / 1000000));
}

function clientSyncEmit(socket) {
    // socket.emit('sync', JSON.stringify({t0:getServerTime()}));
    socket.emit('sync', {t0:getServerTime()});
}

function calculateOffset(t0, t1, t2, t3) { // TODO: Error check!
    return Math.round((((t1 - t0) + (t2 - t3)) / 2));
}

app.get('/', function(req, res) {
    console.log('connection');
    res.send("hi");
});

io.sockets.on('connection', function(socket) {
    ++clientCount;
    clientIDs[socket.id] = getServerTime();
    clientSyncEmit(socket);

    socket.on('client_sync', function() {
        clientSyncEmit(socket);
    });

    socket.on('client_sync_callback', function(syncData) {
        var t3 = getServerTime();
        // var syncData = JSON.parse(data);
        var t0 = syncData.t0; // TODO: Error check!
        var t1 = syncData.t1;
        var t2 = syncData.t2;
        var offset = calculateOffset(t0, t1, t2, t3);
        socket.emit('offset', {offset:offset});
    });

    socket.on('client_play', function() {
        if(!isPlaying) {
            currentSongStartTime = getServerTime() + CLIENT_PLAY_FUTURE_DELAY;
            io.sockets.emit('play', JSON.stringify({name:'name', // TODO
                url:'url', // TODO
                startTime:currentSongStartTime,
                position:getCurrentSongPosition(),
                duration:currentSongDuration,
                positionOffset:CLIENT_PLAY_POSITION_OFFSET,
                retryOffset:CLIENT_PLAY_RETRY_OFFSET}));
        }
        isPlaying = true;
    });

    socket.on('client_stop', function() {
        io.sockets.emit('stop');
        if(isPlaying) {
            currentSongPauseTime = getServerTime();
        }
        isPlaying = false;
    });

    socket.on('disconnect', function() {
        --clientCount;
        delete clientIDs[socket.id]; // TODO: Error check!
    });
});

server.listen(3000, function() {
});