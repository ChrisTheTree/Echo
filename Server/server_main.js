var express = require('express');
var http = require("http");
var url = require('url');
var fs = require('fs');
var path = require('path');
var socketIO = require('socket.io');

var app = express();
var server = http.createServer(app);
var io =  socketIO.listen(server);

var CLIENT_PLAY_FUTURE_DELAY = 2000;
var CLIENT_PLAY_POSITION_OFFSET = -500;
var CLIENT_PLAY_RETRY_OFFSET = 5000;

var STATIC_QUEUE_ID = 'universial_queue';

var app = express();
var server = http.createServer(app);
var io =  socketIO.listen(server);

var clientCount = 0;
var clientIDs = {};

var currentSong;
var isPlaying = false;
var currentSongStartTime = 0;
var currentSongPauseTime = 0;

function Song(songName, filePath, artist, album, timeLength, imageUrl) {
    this.songName = songName;
    this.imageUrl = imageUrl;
    this.filePath = filePath;
    this.artist = artist;
    this.album = album;
    this.timeLength = timeLength;
    this.voteCount = 0;
    this.timeStamp = getServerUTC();
    this.isPlaying = false;
};

function Queue(queueId) {
    this.songs = {};
    this.queueId = queueId;
};

function getCurrentSongPosition() {
    var position = 0;
    if(isPlaying) {
        position = getServerUTC() - currentSongStartTime;
    } else {
        position = currentSongPauseTime - currentSongStartTime;
    }
    if(position > 0) {
        return position;
    } else {
        return 0;
    }
}

function getCurrentSongInfo(){
    var songInfo = {};
    songInfo.song = currentSong;
    songInfo.playAtTime = getServerUTC() + CLIENT_PLAY_FUTURE_DELAY;
    if (isPlaying){
        songInfo.seekTo = getCurrentSongPosition + CLIENT_PLAY_FUTURE_DELAY;
    } else {
        songInfo.seekTo = 0;
    }
    return songInfo;
}

function getServerUTC() {
    return Date.now();
}

var queue = new Queue(STATIC_QUEUE_ID);

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
    var maxUpvote = 0;
    var timeStamp = getServerUTC();
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
    nextSong.timeStamp = getServerUTC();
    nextSong.isPlaying = true;
    return nextSong;
}

io.sockets.on('connection', function(socket) {

    //calls function to emit sync on current socket
    orderedQueue = queue.getQueuedSongs();
    socket.emit('updateQueue', orderedQueue);

    socket.on('addSong', function(songName, filePath, artist, album, timeLength, imageUrl) {
        var newSong = new Song(songName, filePath, artist, album, timeLength, imageUrl);
        queue.songs[filePath] = newSong;
        orderedQueue = queue.getQueuedSongs();
        io.sockets.emit('updateQueue', orderedQueue);
    });

    socket.on('removeSongAtFilePath', function(filePath) {
        delete queue.songs[filePath];
        orderedQueue = queue.getQueuedSongs();
    });

    socket.on('upvoteSongAtFilePath', function(filePath) {
        var song = queue.songs[filePath];
        song.voteCount += 1;
        orderedQueue = queue.getQueuedSongs();
        io.sockets.emit('updateQueue', orderedQueue);
    });

    socket.on('client_play', function() {
        if(isPlaying == false){
            io.sockets.emit('play', getCurrentSongInfo());
            isPlaying = true;
        } else {
            socket.emit('play', getCurrentSongInfo());
        }
    });

    socket.on('client_pause', function() {
        if(isPlaying) {
            currentSongPauseTime = getServerUTC();
            isPlaying = false;
            io.sockets.emit('pause');
        } else {
            socket.emit('pause');
        }
    });

    socket.on('client_sync', function() {
        //calls function to emit sync on current socket
    });
});

server.listen(3000, function() {
});