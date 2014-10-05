var express = require('express');
var http = require('http');
var url = require('url');
var fs = require('fs');
var path = require('path');
var socketIO = require('socket.io');

var CLIENT_PLAY_FUTURE_DELAY = 2000;
var CLIENT_PLAY_RETRY_OFFSET = 5000;
var STATIC_QUEUE_ID = 'universal_queue';

var app = express();
var server = http.createServer(app);
var io =  socketIO.listen(server);

var clientCount = 0;
var clientIDs = {};


//Chris
var currentSongStartTime = 0;
var currentSongPauseTime = 0;

function getCurrentSongPosition() {
    var position = 0;
    if(isPlaying) {
        position = getServerTime() - currentSongStartTime;
    } else {
        position = currentSongPauseTime - currentSongStartTime;
    }
    if(position > 0) {
        return Math.floor(position / 1000);
    } else {
        return 0;
    }
}
//Chris


//-------------------------------------------------------------------------------------Queue Start

var isPlaying = false;
var currentSong;

function Queue(queueId) {
    this.songs = {};
    this.queueId = queueId;
}

var queue = new Queue(STATIC_QUEUE_ID);
//currentSong = new Song('Niggas In Paris', 'https://s3.amazonaws.com/alstroe/850920812.mp3', 'J-Z', 'Throne', 219, 'testURL');
currentSong = new Song('Homecoming', 'https://s3.amazonaws.com/alstroe/1468664291.mp3', 'Kanye West', 'Graduation', 219, 'testURL');
//queue.songs['https://s3.amazonaws.com/alstroe/850920812.mp3'] = currentSong;
queue.songs['https://s3.amazonaws.com/alstroe/1468664291.mp3'] = currentSong;

function Song(songName, filePath, artist, album, timeLength, imageUrl) {
    this.songName = songName;
    this.imageUrl = imageUrl;
    this.filePath = filePath;
    this.artist = artist;
    this.album = album;
    this.timeLength = timeLength;
    this.voteCount = 0;
    this.timeStamp = getServerTime();
}

function getCurrentSongInfo(){
    var songInfo = {};
    songInfo.song = currentSong;
    songInfo.startAt = getServerTime() + CLIENT_PLAY_FUTURE_DELAY;
    return songInfo;
}

Queue.prototype.getQueuedSongs = function() {
    var sortedSongs = [];
    for (var key in this.songs) {
        sortedSongs.push(this.songs[key]);
    }
    sortedSongs.sort(function(a, b) {
        var diff = a[voteCount] - b[voteCount];
        if (diff == 0) {
            diff = a[timeStamp] - b[timeStamp];
        }
        return diff;
    });
    return sortedSongs;
};

Queue.prototype.getNextSong = function() {
    var maxUpvote = -1;
    var timeStamp = getServerTime();
    var nextSong;
    for (var key in this.songs) {
        var song = this.songs[key];
        if (song.voteCount > maxUpvote) {
            maxUpvote = song.voteCount;
            timeStamp = song.timeStamp;
            nextSong = song;
        }
        else if (song.voteCount = maxUpvote && song.timeStamp < timeStamp) {
            timeStamp = song.timeStamp;
            nextSong = song;
        }
    }
    nextSong.voteCount = 0;
    nextSong.timeStamp = getServerTime();
    return nextSong;
};

function playNextSong(){
    io.sockets.emit('stop');
    currentSong = queue.getNextSong();
    var orderedQueue = queue.getQueuedSongs();
    io.sockets.emit('updateQueue', orderedQueue);
    io.sockets.emit('play', getCurrentSongInfo());
    isPlaying = true;
}

//-------------------------------------------------------------------------------------Queue End

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
    res.sendfile("index.html");
});

app.get('/css', function(req, res) {
    res.sendfile("web.css");
});

io.sockets.on('connection', function(socket) {
    ++clientCount;
    clientIDs[socket.id] = getServerTime();
    clientSyncEmit(socket);

//-------------------------------------------------------------------------------------Queue Start

    var orderedQueue = queue.getQueuedSongs();
    socket.emit('updateQueue', orderedQueue);

    socket.on('addSong', function(songName, filePath, artist, album, timeLength, imageUrl) {
        var newSong = new Song(songName, filePath, artist, album, timeLength, imageUrl);
        queue.songs[filePath] = newSong;
        var orderedQueue = queue.getQueuedSongs();
        io.sockets.emit('updateQueue', orderedQueue);
    });

    socket.on('removeSongAtFilePath', function(filePath) {
        delete queue.songs[filePath];
        var orderedQueue = queue.getQueuedSongs();
        io.sockets.emit('updateQueue', orderedQueue);
    });

    socket.on('upvoteSongAtFilePath', function(filePath) {
        var song = queue.songs[filePath];
        song.voteCount += 1;
        var orderedQueue = queue.getQueuedSongs();
        io.sockets.emit('updateQueue', orderedQueue);
    });

    socket.on('client_play', function() {
        if(isPlaying == false){
            // Chris
            currentSongStartTime = getServerTime();
            // Chris
            io.sockets.emit('play', getCurrentSongInfo());
        }
        isPlaying = true;
    });

    socket.on('client_stop', function() {
        if(isPlaying) {
            // Chris
            currentSongPauseTime = getServerTime();
            // Chris
            io.sockets.emit('stop');
        }
        isPlaying = false;
    });

    socket.on('client_next', playNextSong);

//-------------------------------------------------------------------------------------Queue End

    socket.on('client_sync', function() {
        clientSyncEmit(socket);
    });

    socket.on('client_sync_callback', function(syncData) {
        var t3 = getServerTime();
        var t0 = syncData.t0; // TODO: Error check!
        var t1 = syncData.t1;
        var t2 = syncData.t2;
        var offset = calculateOffset(t0, t1, t2, t3);
        socket.emit('offset', {offset:offset});
    });

    socket.on('disconnect', function() {
        --clientCount;
        if (clientCount == 0){
            isPlaying == false;
        }
        delete clientIDs[socket.id]; // TODO: Error check!
    });

    // Siva
    socket.on('startsong', function() {
        socket.emit('timeSync', {t0: getServerTime()});
    });
    // Siva
});

server.listen(3030, function() {
});