//
//  ViewController.swift
//  Echo
//
//  Created by Guorui Wu on 10-04-14.
//  Copyright (c) 2014 TeamFC. All rights reserved.
//

import UIKit
import CoreMedia
import AVFoundation
import Foundation

class ViewController: UIViewController, SocketIODelegate {
    
    @IBOutlet var playButton: UIButton!
    @IBOutlet var songNameLabel: UILabel!
    @IBOutlet var albumNameLabel: UILabel!
    @IBOutlet var playButtonLabel: UILabel!
    
    var socket : SocketIO = SocketIO()
    let host = "10.42.0.1"
    let port = 3030
    var songPath = "https://s3.amazonaws.com/alstroe/850920812.mp3"
    var time = mach_timebase_info(numer: 0, denom: 0)
    var player = AVPlayer()
    var playerItem = AVPlayerItem()
    var seekToTime = CMTimeMake(0, 1)
    var offset: Double = 0
    var delay: Double = 0
    let offsetTolerance: CLong = 10000
    let numberOfOffsets: CLong = 10
    let syncDelay: UInt32 = 40
    var offsetCounter = 0
    var offsets = [CLong](count: 10, repeatedValue: 0.0)
    var synchronized: Bool = false
    
    override func viewDidLoad() {
        super.viewDidLoad()
        self.songNameLabel.text = ""
        self.albumNameLabel.text = ""
        
        self.socket = SocketIO(delegate: self)
        self.socket.connectToHost(host, onPort: port)
        mach_timebase_info(&time)
        
        self.playerItem = AVPlayerItem(URL: NSURL(string: self.songPath))
        self.player = AVPlayer(playerItem: playerItem)
        weak var weakSelf = self
        waitThenRunOnMain(10.0) {
            if let strongSelf = weakSelf {
                strongSelf.player.prerollAtRate(1.0, completionHandler: nil)
            }
        }
    }
    
    override func didReceiveMemoryWarning() {
        super.didReceiveMemoryWarning()
        // Dispose of any resources that can be recreated.
    }
    
    override func prefersStatusBarHidden() -> Bool {
        return true
    }
    
    func socketIO(socket: SocketIO!, didReceiveMessage packet: SocketIOPacket!) {
        println("received message")
    }
    
    func socketIO(socket: SocketIO!, didReceiveJSON packet: SocketIOPacket!) {
        println("received JSON")
    }
    
    func socketIO(socket: SocketIO!, didReceiveEvent packet: SocketIOPacket!) {
        let t1: Double = Double(mach_absolute_time() * UInt64(time.numer) / UInt64(time.denom)) / 1000000
        let name = packet.name
        if name == "sync" {
            let dict = packet.args[0] as NSDictionary
            let t0: CLong = dict.objectForKey("t0") as CLong
            let t2: Double = Double(mach_absolute_time() * UInt64(time.numer) / UInt64(time.denom)) / 1000000
            self.socket.sendEvent("client_sync_callback", withData: ["t0": t0, "t1": t1, "t2": t2])
        } else if name == "offset" {
            let dict = packet.args[0] as NSDictionary
            self.offset = dict.objectForKey("offset") as Double
            processOffsets(CLong(self.offset))
        } else if name == "play" {
            self.playButtonLabel.text = "Stop"
            
            let dict = packet.args[0] as NSDictionary
            let songDict = dict["song"] as NSDictionary
            let startAt = dict["startAt"] as NSNumber

            
            
            var t = mach_timebase_info(numer: 0, denom: 0)
            mach_timebase_info(&t)
            var clientTime = (Double(mach_absolute_time() * UInt64(t.numer)) / Double(UInt64(t.denom)) / 1000000)
            var time: CLong = CLong(clientTime - self.offset)
            self.delay = Double((startAt.intValue - time)) / 1000.0
            
            self.songNameLabel.text = songDict["songName"] as NSString
            self.albumNameLabel.text = songDict["album"] as NSString
            
            println("clientTime is \(clientTime)")
            println("offset is \(self.offset)")
            println("delay is \(delay)")
            
            weak var weakSelf = self
            waitThenRunOnMain(Double(delay)) {
                if let strongSelf = weakSelf {
                    strongSelf.playSong()
                }
            }
        } else if name == "stop" {
            self.playButtonLabel.text = "Play"
            self.player.pause()
            self.player.seekToTime(self.seekToTime, toleranceBefore: kCMTimeZero, toleranceAfter: kCMTimeZero)
        } else if name == "updateQueue" {
            
        }
        
    }
    
    func playSong() {
        self.player.play()
        println("playing")
    }
    
    @IBAction func playButtonPressed(sender: AnyObject) {
        println("play button pressed")
        if self.playButtonLabel.text == "Play" {
            self.socket.sendEvent("client_play", withData:nil)
        } else {
            self.socket.sendEvent("client_stop", withData:nil)
        }
    }
    
    func waitThenRunOnMain(delay:Double, closure:()->()) {
        dispatch_after(
            dispatch_time(
                DISPATCH_TIME_NOW,
                Int64(delay * Double(NSEC_PER_SEC))
            ),
            dispatch_get_main_queue(), closure)
    }
    
    func getOffset(offsets: [CLong]) -> CLong {
        var counters = [CLong](count: 10, repeatedValue: 0.0)
        var results = [CLong](count: 10, repeatedValue: 0.0)
        for var i = 0; i < offsets.count; ++i {
            var counter: CInt = 0
            var sum: CLong = 0
            for otherOffset in offsets {
                if abs(offsets[i] - otherOffset) <= offsetTolerance {
                    counter++
                    sum += CLong(counter)
                }
            }
            counters[i] = CLong(counter)
            if counter != 0 {
                results[i] = CLong(Double(sum) / Double(counter))
            }
        }
        var max: CInt = 0
        var maxIndex = 0
        for var i = 0; i < counters.count; ++i {
            max = CInt(counters[i])
            maxIndex = i
        }
        if max == 0 {
            return CLong(0)
        } else {
            return CLong(results[maxIndex])
        }
    }
    
    func processOffsets(offset: CLong) {
        if self.offsetCounter < numberOfOffsets {
            self.offsets[self.offsetCounter] = offset
            ++offsetCounter
            if offsetCounter < numberOfOffsets {
//                sleep(self.syncDelay)
                self.socket.sendEvent("client_sync", withData: nil)
            }
        }
        if self.offsetCounter >= self.numberOfOffsets {
            self.offset = Double(getOffset(offsets))
            self.synchronized = true
        }
    }
}

