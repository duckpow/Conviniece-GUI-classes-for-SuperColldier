// gui stuff to hopefully make my life bearable...
/*
MyGui {

	*new{
		^super.new;
	}

}
*/

ChannelStrip { //mostly a superclass
	var <>numChan;
	var <>outBus;
	var inBus;
	var serverRef;
	var sName;
	var synth, synthParameters, synthGroup;
	var view;
	var vertOffset, channelHeight;
	var channelText, outputChannelBox;

	*new{
		arg parentView, synthName, numChannels=2, outputBus=0, inputBus, verticalOffset=0, horizontalOffset=0;
		^super.new.init(parentView, synthName, numChannels, outputBus, inputBus, verticalOffset, horizontalOffset);
	}

	init{
		arg parentView, synthName, numChannels, outputBus, inputBus, verticalOffset, horizontalOffset;

		serverRef = Server.default;
		channelHeight = 220;
		view = View(parentView,
			Rect(horizontalOffset,verticalOffset,500,channelHeight)
		);
		sName = synthName;
		numChan = numChannels;
		outBus = outputBus;
		inBus = inputBus;
		vertOffset = 5;
		this.initChannel();
	}

	initChannel{
		this.createUI();
	}

	createUI {
		"sry to keep you waiting".postln;
	}

	buttonCreator {
		arg parentView, offset, states, action;
		var butt;
		butt = Button(parentView,
			Rect(offset[0], offset[1], 45, 45)
		).states_(
			states
		).action_(
			action
		);
		^butt;
	}

	set{
		"somfing".postln;
	}

	// MIDI bind functions

	bindMIDI{
		arg type, parameter, number, interpType, mapLo, mapHi;
		MIDIClient.initialized.if({
			var escapedType = type.asSymbol;
			case
			{(escapedType == \note).or(escapedType == \noteOn)}{
				var defName = ("note" ++ number.asString).asSymbol;
				(parameter.isMemberOf(Function)).if({
					MIDIdef.noteOn(defName, parameter, number);
				},{
					MIDIdef.noteOn(defName,{
						arg value, nn, chan, src;
						var val;
						(interpType == \exp).if({
							val = value.linexp(0,127,mapLo, mapHi);
						},{
							val = value.linlin(0,127,mapLo, mapHi);
						});
						AppClock.sched(0,{
							this.set(parameter.asSymbol, val).postln;
						});
					},number);
				})
			}
			{escapedType == \cc}{
				var defName = ("cc" ++ number.asString).asSymbol;
				MIDIdef.cc(defName,{
					arg value, num, chan, src;
					var val;
					(interpType == \exp).if({
						val = value.linexp(0,127,mapLo, mapHi);
					},{
						val = value.linlin(0,127,mapLo, mapHi);
					});
					AppClock.sched(0,{
						this.set(parameter.asSymbol, val);
					});
				},number);
			}
		},{
			"MIDI not initialized".postln;
		});
	}

	getBottomPixel {
		^ channelHeight;
	}

}

ChannelStripSeq : ChannelStrip {
	var playButton;
	var synthGroup;
	var attack, release, triggerSpeed;
	var attackSlider, releaseSlider, triggerSlider;
	var attackSliderLabel, releaseSliderLabel, triggerSliderLabel;

	createUI{
		var textHeight = 20;
		channelText = StaticText(view,
			Rect(10,vertOffset,200,textHeight)
		).string_(sName.asString.toUpper ++ " Channels: " ++ numChan.asString);

		outputChannelBox = EZText(view,
			Rect(200, vertOffset, 100, textHeight),"outBus",{
				|ar| ar.value.isMemberOf(Integer).or(
					ar.value.isMemberOf(Bus)
				).if({
					outBus = ar.value;
					synth.set(\out, outBus);
				},{
					ar.value = outBus;
				})
		},outBus);

		synthGroup = Group.new;

		attack = PatternProxy.new.source = 0.01;
		release = PatternProxy.new.source = 1;
		triggerSpeed = PatternProxy.new.source = 1;

		playButton = this.buttonCreator(view,
			[200,textHeight+vertOffset],
			[
				[">>",Color.green(0.7)],
				["X",Color.red(0.8)]
			],{
				arg button;
				button.value.asBoolean.if({
					this.playPattern;
				},{
					this.stopPattern;
				});

			}
		);

		attackSlider = Slider(view,
			Rect(10,vertOffset+(textHeight),40,120)
		).value_(0.01).action_({|sl|
			attack.source = sl.value;
		});

		attackSliderLabel = StaticText(view,
			Rect(10,vertOffset+(textHeight)+120,40,textHeight)
		).string_("Attack");

		releaseSlider = Slider(view,
			Rect(50,vertOffset+textHeight,40,120)
		).value_(0.25).action_({|sl|
			release.source = sl.value.linlin(0,1,0.1,5);
		});

		releaseSliderLabel = StaticText(view,
			Rect(50,vertOffset+textHeight+120,40,textHeight)
		).string_("Release");

		triggerSlider = Slider(view,
			Rect(90,vertOffset+textHeight,40,120)
		).value_(0.25).action_({|sl|
			triggerSpeed.source = sl.value.linlin(0,1,1,0.1);
		});

		triggerSliderLabel = StaticText(view,
			Rect(90,vertOffset+textHeight+120,40,textHeight)
		).string_("trSpeed");

	}

	play{
		playButton.valueAction_((playButton.value+1)%2);
	}

	playPattern{
		Pdef(\test,
			Pbind(
				\instrument, sName.asSymbol,
				\out, outBus,
				\attack, attack,
				\release, release,
				\freq, Prand([42,48,49,55,60,61],inf).midicps,
				\dur, Pwhite(0.3*triggerSpeed*2,3*triggerSpeed*2,inf)
			)
		).play;
	}

	stopPattern{
		Pdef(\test).stop;
	}

	set{
		arg param, val;
		case
		{param.asSymbol==\attack}{
			attackSlider.valueAction_(val);
		}
		{param.asSymbol==\release}{
			releaseSlider.valueAction_(val);
		}
		{param.asSymbol==\triggerSpeed}{
			triggerSlider.valueAction_(val);
		}
	}
}

ChannelStripInOut : ChannelStrip {
	var playButton;
	var inputChannelBox;
	var mstrSlider, delayAmountSlider, delayFbSlider, hiCutSlider;
	var mstrSliderName, delayAmountSliderName, delayFbSliderName, hiCutSliderLabel;

	createUI{
		var textHeight = 20;
		channelText = StaticText(view,
			Rect(10,vertOffset,200,textHeight)
		).string_(sName.asString.toUpper ++ " Channels: " ++ numChan.asString);

		inputChannelBox = EZText(view,
			Rect(10,vertOffset+(textHeight), 100, textHeight),"inBus",{
				|ar| ar.value.isMemberOf(Integer).or(
					ar.value.isMemberOf(Bus)
				).if({
					inBus = ar.value;
					synth.set(\in, inBus);
				},{
					ar.value = inBus;
				})
		},inBus);

		outputChannelBox = EZText(view,
			Rect(10,vertOffset+(textHeight*2), 100, textHeight),"outBus",{
				|ar| ar.value.isMemberOf(Integer).or(
					ar.value.isMemberOf(Bus)
				).if({
					outBus = ar.value;
					synth.set(\out, outBus);
				},{
					ar.value = outBus;
				})
		},outBus);

		mstrSlider = Slider(view,
			Rect(40,vertOffset+(textHeight*3),40,120)
		).value_(0.8).action_({|sl|
			synth.set(\vol,(sl.value));
		});

		mstrSliderName = StaticText(view,
			Rect(40,vertOffset+(textHeight*3)+120,40,textHeight)
		).string_("Mstr");

		delayAmountSlider = Slider(view,
			Rect(80,vertOffset+(textHeight*3),40,120)
		).value_(0).action_({|sl|
			synth.set(\dlyAmt, sl.value);
		});

		delayAmountSliderName = StaticText(view,
			Rect(80,vertOffset+(textHeight*3)+120,40,textHeight)
		).string_("dAmt");

		delayFbSlider = Slider(view,
			Rect(120,vertOffset+(textHeight*3),40,120)
		).value_(0).action_({|sl|
			synth.set(\dlyFeedback, sl.value);
		});

		delayFbSliderName = StaticText(view,
			Rect(120,vertOffset+(textHeight*3)+120,40,textHeight)
		).string_("dFb");

		hiCutSlider = Slider(view,
			Rect(160,vertOffset+(textHeight*3),40,120)
		).value_(0).action_({|sl|
			synth.set(\hiCutFreq, sl.value.linexp(0,1,120,20000));
		});

		hiCutSliderLabel = StaticText(view,
			Rect(160,vertOffset+(textHeight*3)+120,40,textHeight)
		).string_("hiCut");

		synth = Synth(\mstrChannel,[\out, outBus, \in, inBus]);
	}

	set {
		arg param, val;
		case
		{param.asSymbol==\vol}{
			mstrSlider.valueAction_(val);
		}
		{param.asSymbol==\dlyAmt}{
			delayAmountSlider.valueAction_(val);
		}
		{param.asSymbol==\dlyFeedback}{
			delayFbSlider.valueAction_(val);
		}
		{param.asSymbol==\hiCutFreq}{
			hiCutSlider.valueAction_(val);
		}
		//synth.set(param.asSymbol, val);
	}

	free {
		synth.free;
	}
}


ChannelStripWithBuffer : ChannelStrip {
	var buffer;
	var sliders;
	var <>playButton, randButton, loadButton, bufferNameText;

	createUI {
		var horizButtonOffset, textHeight=20;
		buffer = nil;
		sliders = Dictionary.new;
		synthParameters = ();
		// UI creation
		channelText = StaticText(view,
			Rect(10,vertOffset,200,textHeight)
		).string_(sName.asString.toUpper ++ " Channels: " ++ numChan.asString);
		outputChannelBox = EZText(view,
			Rect(200,vertOffset, 100, textHeight),"outBus",{
				|ar| ar.value.isMemberOf(Integer).or(
					ar.value.isMemberOf(Bus)
				).if({
					outBus = ar.value;
				},{
					ar.value = outBus;
				})
		},outBus);

		vertOffset = vertOffset + textHeight;

		this.sliderFabric.value();

		horizButtonOffset = 10+(sliders.size*50);

		playButton = this.buttonCreator(view,
			[horizButtonOffset,vertOffset],
			[
				[">>",Color.green(0.7)],
				["X",Color.red(0.8)]
			], {
				arg button;
				button.value.asBoolean.if({
					this.playSynth;
				},{
					this.stop;
				});

			}
		);

		randButton = this.buttonCreator(view,
			[horizButtonOffset,50+vertOffset],[["Random\nPos"]]
		);
		randButton.action_({
			arg button;
			this.randSamplePos();
			//sliders[\samplePos].valueAction_(rand(1.0));
		});

		loadButton = this.buttonCreator(view,
			[horizButtonOffset,100+vertOffset], [["Load \nsample"]]
		);
		loadButton.action_({
			this.getNewBuffer(1); //arg is channel count
		});
		bufferNameText = StaticText(view,
			Rect(horizButtonOffset,145+vertOffset,90,45)
		).string_("test");
		this.updateTextView();
	}

	playSynth {
		this.updateAllSynthParams(); //poll all sliders before playing
		buffer.notNil.if({
			synthParameters[\bufNum] = buffer;
			synthParameters[\out] = outBus;
			synth = Synth(sName.asSymbol, synthParameters.getPairs);
		},{
			"Please load a buffer".postln;
		});

	}

	play {
		playButton.valueAction_((playButton.value+1)%2);
	}

	stop {
		synth.release;
		synth = nil;
	}

	set { // set sliders value with action
		arg parameter, value;
		var sl = sliders[parameter.asSymbol];
		sl.notNil.if({ //if not nil value exist for this synth
			sl.valueAction_(value);
		},{
			(parameter == "bufNum").if({
				synth.set(\buf, value);
			},{
				"Parameter not found\nFurther implementation impending".postln;
			});
		});
	}

	randSamplePos {
		sliders[\samplePos].valueAction_(rand(1.0));
	}



	sliderFabric{
		// create and bind sliders to a synth
		// Requires its parameters is registered in ControlSpec. Skips otherwise

		SynthDescLib.global[sName.asSymbol].controlNames.do({
			arg name, index;
			var spec, action;
			spec = name.asSymbol.asSpec;
			action = {|view| synth.set(name.asSymbol, view.value);};
			spec.notNil.if({
				sliders.add(name -> EZSlider(
					view,
					Rect(10+(sliders.size*50),vertOffset,30,180),
					name, spec, action, layout: 'vert'
				));
			});
		});
	}

	getNewBuffer {
		arg numChannels; //only one channel allowed in GrainBuf
		var channels;
		channels = Array.series(numChannels);

		serverRef.serverRunning.if({
			Dialog.openPanel({|soundPath|
				var path = PathName(soundPath);
				buffer.free;
				buffer = Buffer.readChannel(serverRef, path.fullPath, channels: channels);
				this.updateTextView(path.fileName);
			}, multipleSelection: false);
		},{"Please boot the server".postln});
		//^buf;
	}

	createNewBuffer {
		arg numChannels; //only one channel allowed in GrainBuf
		var channels;
		channels = Array.series(numChannels);

		serverRef.serverRunning.if({
			buffer.free;
			buffer = Buffer.alloc(serverRef, serverRef.sampleRate * 2, numChannels: channels);
			this.updateTextView("Buffer Loaded");
		},{"Please boot the server".postln});
		//^buf;
	}

	updateTextView {
		arg text;
		text.notNil.if({
			text.isKindOf(String).if({
				bufferNameText.string_(text);
				},{
				bufferNameText.string_(text[1])
			});
			},{
			bufferNameText.string_("No buffer loaded");
		});
	}

	// Synth helper functions
	updateSynthParam {
		arg slider;
		synthParameters[slider.labelView.string.asSymbol] = slider.value;
	}

	updateAllSynthParams {
		sliders.do({
			arg sl;
			this.updateSynthParam(sl);
		});
	}

	getRightMostPixel{
		^ 10+(sliders.size*50)+45;
	}
}