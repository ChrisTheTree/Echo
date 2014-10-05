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

class ViewController: UIViewController, SocketIODelegate {
    
    @IBOutlet var playButton: UIButton!
    
    var socket : SocketIO = SocketIO()
    var host = "10.42.0.1"
    var port = 3030
    var songPath = "https://s3.amazonaws.com/alstroe/850920812.mp3"
    var time = mach_timebase_info(numer: 0, denom: 0)
    var player = AVPlayer()
    var playerItem = AVPlayerItem()
    var seekToTime = CMTimeMake(0, 1)
    var offset: Double = 0
    var delay: CLong = 0
    
    override func viewDidLoad() {
        super.viewDidLoad()
        self.playButton.enabled = false
        self.playerItem = AVPlayerItem(URL: NSURL(string: self.songPath))
        self.player = AVPlayer(playerItem: playerItem)
        weak var weakSelf = self
        waitThenRunOnMain(5.0) {
            if let strongSelf = weakSelf {
                strongSelf.playButton.enabled = true
                strongSelf.player.prerollAtRate(1.0, completionHandler: nil)
            }
        }
        self.socket = SocketIO(delegate: self)
        self.socket.connectToHost(host, onPort: port)
        mach_timebase_info(&time)
    }
    
    override func didReceiveMemoryWarning() {
        super.didReceiveMemoryWarning()
        // Dispose of any resources that can be recreated.
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
            var dict = packet.args[0] as NSDictionary
            let t0: CLong = dict.objectForKey("t0") as CLong
            let t2: Double = Double(mach_absolute_time() * UInt64(time.numer) / UInt64(time.denom)) / 1000000
            self.socket.sendEvent("client_sync_callback", withData: ["t0": t0, "t1": t1, "t2": t2])
        } else if name == "offset" {
            var dict = packet.args[0] as NSDictionary
            self.offset = dict.objectForKey("offset") as Double
            var t = mach_timebase_info(numer: 0, denom: 0)
            mach_timebase_info(&t)
            var clientTime = (Double(mach_absolute_time() * UInt64(t.numer)) / Double(UInt64(t.denom)) / 1000000)
            var time: CLong = CLong(clientTime - self.offset)
            var futureTime: CLong = (time + 5000) / 10000 * 10000
            if futureTime - time < 5000 {
                futureTime += 10000
            }
            clientTime = Double(mach_absolute_time() * UInt64(t.numer)) / Double(UInt64(t.denom)) / 1000000
            self.delay = CLong(futureTime + CLong(self.offset) - CLong(clientTime)) / 1000.0
            println("offset is \(self.offset)")
            weak var weakSelf = self
        } else if name == "play" {
            let dict = packet.args[0] as NSDictionary
            let songDict = dict["song"] as NSDictionary
            let playAtTime = dict["playAtTime"] as NSNumber
            let seekTo = dict["seekTo"] as NSNumber
            self.seekToTime = CMTimeMake(Int64(seekTo.intValue), 1)
            
            var t = mach_timebase_info(numer: 0, denom: 0)
            mach_timebase_info(&t)
            var clientTime = (Double(mach_absolute_time() * UInt64(t.numer)) / Double(UInt64(t.denom)) / 1000000)
            var time: CLong = CLong(clientTime - self.offset)
            self.delay = CLong(playAtTime.intValue) - time
            
            weak var weakSelf = self
            waitThenRunOnMain(Double(delay)) {
                if let strongSelf = weakSelf {
                    strongSelf.playSong()
                    println("test")
                }
            }
        } else if name == "pause" {
            
        } else if name == "updateQueue" {
            
        }
        
    }
    
    func playSong() {
        self.player.play()
        self.player.seekToTime(seekToTime, toleranceBefore: kCMTimeZero, toleranceAfter: kCMTimeZero)
        println("playing")
    }
    
    @IBAction func playButtonPressed(sender: AnyObject) {
        println("play button pressed")
        self.socket.sendEvent("client_play", withData:nil)
        println("delay is \(self.delay)")
        

    }
    
    func waitThenRunOnMain(delay:Double, closure:()->()) {
        dispatch_after(
            dispatch_time(
                DISPATCH_TIME_NOW,
                Int64(delay * Double(NSEC_PER_SEC))
            ),
            dispatch_get_main_queue(), closure)
    }
    
    func JSONStringify(jsonObj: AnyObject) -> String {
        var e: NSError?
        let jsonData: NSData! = NSJSONSerialization.dataWithJSONObject(
            jsonObj,
            options: NSJSONWritingOptions(0),
            error: &e)
        if e != nil {
            return ""
        } else {
            return NSString(data: jsonData, encoding: NSUTF8StringEncoding)
        }
    }
}

