var express = require('express');
var http = require("http");
var url = require('url');
var fs = require('fs');
var path = require('path');
var socketIO = require('socket.io');

var app = express();
var server = http.createServer(app);
var io =  socketIO.listen(server);

app.use(express.static(path.join(__dirname, '/')));

app.get('/', function(req, res) {
    console.log('connection');
    res.send("hi");
});

function oneSong(songName, filePath, artist, album, imageUrl) {
	this.songName = songName;
	this.imageUrl = imageUrl;
    this.filePath = filePath;
    this.artist = artist;
    this.album = album;
	this.voteCount = 0;
    this.timeStamp = Date.now();
    this.isPlaying = false;
};

var queue = {};

queue.prototype.getQueuedSongs = function() {
    var sortedSongs = [];
    for (var key in queue) {
        sortedSongs.push(queue[key]);
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

queue.prototype.getNextSong = function() {
    var maxUpvote = 0;
    var timeStamp = Date.now();
    var nextSong;
    for (var key in queue) {
        var song = queue[key];
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
    nextSong.timeStamp = Date.now();
    nextSong.isPlaying = true;
    return nextSong;
}

io.sockets.on('connection', function(socket) {

    orderedQueue = queue.getQueuedSongs();
    socket.emit('updateQueue', orderedQueue);

    socket.on('addSong', function(songName, filePath, artist, album, imageUrl) {
        var newSong = new oneSong(songName, filePath, artist, album, imageUrl);
        queue[filePath] = newSong;
        orderedQueue = queue.getQueuedSongs();
        io.sockets.emit('updateQueue', orderedQueue);
    });

    socket.on('removeSongAtFilePath', function(filePath) {
        delete queue[filePath];
        orderedQueue = queue.getQueuedSongs();
    });

    socket.on('upvoteSongAtFilePath', function(filePath) {
        var song = queue[filePath];
        song.voteCount += 1;
        orderedQueue = queue.getQueuedSongs();
        io.sockets.emit('updateQueue', orderedQueue);
    });
});