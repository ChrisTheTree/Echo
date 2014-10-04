//
//  ViewController.swift
//  Echo
//
//  Created by Guorui Wu on 10-04-14.
//  Copyright (c) 2014 TeamFC. All rights reserved.
//

import UIKit

class ViewController: UIViewController, SocketIODelegate {
    
    var socket : SocketIO = SocketIO()
    var host = "10.42.0.1"
    var port = 3000
    var songPath = "http://mp3dos.com/assets/songs/18000-18999/18615-niggas-in-paris-jay-z-kanye-west--1411570006.mp3"
    
    override func viewDidLoad() {
        super.viewDidLoad()
        self.socket = SocketIO(delegate: self)
        self.socket.connectToHost(host, onPort: port)
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
        let json: AnyObject! = packet.dataAsJSON()
        println("received event")
        println(JSONStringify(json))
        weak var weakSelf = self
        waitThenRunOnMain(0.25) {
            if let strongSelf = weakSelf {
                strongSelf.playSong()
            }
        }
    }
    
    func playSong() {
        
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

