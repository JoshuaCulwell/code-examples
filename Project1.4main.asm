;-------------------------------------------------------------------------------
;Joshua Culwell
;EELE 371 - Project 1.4
;10/7/2022
;
; Define Port5.0 - Port5.3 as Digital Input
; Define Port6.0 - Port6.3 as "Process Output"
; Define Port2.0 - Port2.2 as Material Input
; Define Port1.7 - ERROR output
; Define R6		 - Material Type Address
; Define R7 R8	 - Delay Counter 1 and 2
; Define R9		 - Material Type Display
; Define R10     - Counter
; Define R11     - Total Scoop Count
;-------------------------------------------------------------------------------
            .cdecls C,LIST,"msp430.h"       ; Include device header file
            
;-------------------------------------------------------------------------------
            .def    RESET                   ; Export program entry-point to
                                            ; make it known to linker.
;-------------------------------------------------------------------------------
            .text                           ; Assemble into program memory.
            .retain                         ; Override ELF conditional linking
                                            ; and retain current section.
            .retainrefs                     ; And retain any sections that have
                                            ; references to current section.

;-------------------------------------------------------------------------------
RESET       mov.w   #__STACK_END,SP         ; Initialize stackpointer
StopWDT     mov.w   #WDTPW|WDTHOLD,&WDTCTL  ; Stop watchdog timer


;-------------------------------------------------------------------------------
; Main loop here
;-------------------------------------------------------------------------------
init:
	mov.w	#000FFh, R4
	mov.w	#00000h, R5
	;Material Type Address
	mov.w	#02000h, R6
	;Delay Counters
	mov.w	#00000h, R7
	mov.w	#00000h, R8
	;Material Type Display
	mov.w	#0AAAAh, R9
	;Scoop Counters
	mov.w	#00000h, R10
	mov.w	#00000h, R11
	;Configure Switch 2 (pin 2.3)
	bic.b	#00001000b, &P2DIR
	bis.b	#00001000b, &P2REN
	bis.b	#00001000b, &P2OUT ;set as pull-up
	;Configure Switch 1 (pin 4.1)
	bic.b	#00000010b, &P4DIR
	bis.b	#00000010b, &P4REN
	bis.b	#00000010b, &P4OUT ;set as pull-up

	;Configure LED 2
	bis.b	#01000000b, &P6DIR
	bic.b	#01000000b, &P6OUT
	;Configure LED 1
	bis.b	#00000001b, &P1DIR
	bic.b	#11111111b, &P1OUT

	;Define Port5.0-Port5.3 as digital input
	bic.b	#00001111b, &P5DIR
	bis.b	#00001111b, &P5REN
	bic.b	#00001111b, &P5OUT ;set as pull down

	;Define Port2.0-Port2.2 as digital input
	bic.b	#00000111b, &P2DIR
	bis.b	#00000111b, &P2REN
	bic.b	#00000111b, &P2OUT ;set as pull down

	;Define Port6.0-Port6.3 as "Process output"
	bis.b	#01001111b, &P6DIR
	bic.b	#11111111b, &P6OUT

	;Define port1.7 as ERROR
	bis.b	#10000000b, &P1DIR
	bic.b	#10000000b, &P1OUT

	bic.b	#LOCKLPM5, &PM5CTL0

main:

ifSW2:
	mov.b	&P2IN, R5					;Move P2IN into R5
	bic.b	#11110111b, R5				;Clear all but switch bits
	tst		R5							;Test R5
	jnz		ifSW1						;Skips to next thing if switch isn't pressed
holdSW2:
	mov.b	&P2IN, R5					;Move P2IN into R5
	bic.b	#11110111b, R5				;Clear all but switch bits
	tst		R5							;Test R5
	jz		holdSW2						;Makes it so that it only counts once per click and not like a million times if the switch is held down

	mov.w	R10, R5						;9 Scoop max and indicator
	sub.w	#9, R5						;Subtract 9 from scoop count
	tst		R5							;^
	jnz		incrScoop					;If 9-scoop count is not zero go to add 1
	bis.b	#10000000b, &P1OUT			;Turn on ERROR LED
	jmp		ifSW1						;Continue to Switch 1

incrScoop:								;
	add.w	#1, R10						;Incriment Scoop Count

ifSW1:
	mov.b	&P4IN, R5					;Test if pressed
	bic.b	#11111101b, R5				;
	tst		R5							;
	jnz		main						;If not pressed return to main
	add.w	R10, R11					;If pressed add scoop count to total
	mov.b	R10, R5						;Check if scoop count is zero
	tst		R5							;
	bic.b	#10000000b, &P1OUT			;Turn off ERROR output
	jnz		flashLED2					;If scoop count is zero flash LED1

flashLED1:
	bis.b	#00000001b, &P1OUT			;Turn on red LED
	mov.w	#0FFFFh, R5					;Put FFFF in R5
LED1ON:
	dec.w	R5							;Decriment R5
	jnz 	LED1ON						;If not zero loop
	bic.b	#00000001b, &P1OUT			;Turn off LED2
	mov.w	#0FFFFh, R5					;Put FFFF back into R6
LED1OFF:
	dec.w	R5							;Decriment R5
	jnz		LED1OFF						;Loop if not zero
	jmp		main

flashLED2:
	tst		R10							;Check the scoop count
	jz 		materialProcessing			;If it is zero continue to UserInput
	dec.w	R10							;If not decriment and flash

	bis.b	#01000000b, &P6OUT			;Turn on Green LED
	mov.w	#0FFFFh, R5					;Put FFFF in R5
DelayOn:
	dec.w	R5							;Decriment R5
	jnz 	DelayOn						;If not zero loop
	bic.b	#01000000b, &P6OUT			;Turn off LED2
	mov.w	#0FFFFh, R5					;Put FFFF back into R5
DelayOff:
	dec.w	R5							;Decriment R5
	jnz		DelayOff					;Loop if not zero
	jmp		flashLED2					;Jump to Done unconditionaly


materialProcessing:
	mov.b	&P5IN, R5					;Read in R5
	bic.b	#11110000b, R5				;Clear bits to test
if0000:									;Hold for decision
	tst		R5
	jz		materialProcessing
if0001:										;Move material forward
	bic.b	#00000001b, R5				;Clear bit to see if its the first one
	tst 	R5							;test
	jnz		if0011						;If it isn't then jump to the next if
	bis.b	#00000001b, &P6OUT			;Turn on the first LED on conveyer
	bic.b	#11111110b, &P6OUT			;Clears everything else
	mov.w	#8, R13						;Set for loop timing -- 2 Seconds
	jmp		moveForward					;if input is 0001 move material forward
holdMaterial0001:						;Once its done wait for next input
	mov.b	&P5IN, R5					;Test R5 for pin 1
	bic.b	#11111101b, R5				;^
	tst		R5							;^
	jz		holdMaterial0001			;if not loop till it is
if0011:										;Return actuator to home position
	bic.b	#00000010b, R5				;Check to make sure its the correct one
	tst		R5							;^
	jnz		if0111						;if not go to the next
	bis.b	#00001000b, &P6OUT			;If it is turn on the 4th LED
	bic.b	#11110111b, &P6OUT			;Clears everything else
	mov.w	#2, R13
	jmp		moveBackward				;Move backward
holdMaterial0011:
	mov.b	&P5IN, R5					;Test for the correct input
	bic.b	#11110011b, R5				;^
	tst		R5							;^
	jz		holdMaterial0011			;if it is not correct then hold
if0111:										;If input is 0111 move R10 into 0x2020
	bic.b	#00000100b, R5				;Clear to test correct input
	tst		R5							;^
	jnz		if1111						;If incorrect move on
	mov.w	R11, &02020h				;Update 0x2020 with total count
holdMaterial0111:						;
	mov.b	&P5IN, R5					;Test input
	bic.b	#11110111b, R5				;^
	tst		R5							;^
	jnz		holdMaterial0111			;If incorrect hold till correct
if1111:										;Update chosen material
	bic.b	#00001000b, R5				;Check if correct input
	tst		R5							;^
	jz		displayMaterial				;If incorrect (somehow?) return to beginning of if statements
	jmp		main						;After updating return to main

moveForward:							;Move conveyer/actuator forward
	mov.w	R13, R12					;Set for loop times
forMoveForward:							;For loop
	mov.w	#0FFFFh, R5					;Delay value
delayMoveForward:						;
	dec.w 	R5							;Decriment till 0
	jnz		delayMoveForward			;^
	dec.w	R12							;^
	jnz 	forMoveForward				;if for loop isn't finished redo delay

	mov.b	&P6OUT, R5					;Check position
	bic.b	#11110000b, R5				;^
	tst		R5							;^
	jz		holdMaterial0001			;If off go to hold
	rla.b	&P6OUT
	mov.b	&P6OUT, R5
	bic.b	#11111000b, R5				;if one before off cut time in half
	tst 	R5
	jz		offLastForward				;
	jmp		moveForward					;Loop till conditions above are met

offLastForward:							;Last light is on
	mov.w	#4, R13						;Cut time in half -- 1 second
	jmp 	moveForward					;Loop through one last time to turn it off


moveBackward:							;Move conveyer/actuator forward
	mov.w	R13, R12					;Set for loop times
forMoveBackward:							;For loop
	mov.w	#0FFFFh, R5					;Delay value
delayMoveBackward:						;
	dec.w 	R5							;Decriment till 0
	jnz		delayMoveBackward			;^
	dec.w	R12							;^
	jnz 	forMoveBackward				;if for loop isn't finished redo delay

	mov.b	&P6OUT, R5					;Test Port6
	bic.b	#11110000b, R5				;^
	tst		R5							;^
	jz		flipBelts					;If all LEDs are off flip the belt and move on
	rra.b	&P6OUT						;If not continue rotating bits
	bic.b	#11110000b, &P6OUT			;Turn off any extra things that may have been turned on while rotating
	jmp		moveBackward				;Loop
flipBelts:								;
	xor.w	#0FFFFh, R4					;Flip belts (swap R4)
	jmp		holdMaterial0011			;Wait for next input

displayMaterial:						;Displays material value
	;Pin2.0-2.2							;
	mov.w	&P2IN, R5					;Check material input
	bic.b	#11111000b, R5				;Clear extra inputs
	rla.w	R5							;Rotate it to the left to multiply by 2
	mov.w	#Reserve0, R12				;Move address of material types into R12
	add.w	R5, R12						;Add the shift to R12
	mov.w	@R12, R9					;put material value at address held in R12 in R9

	jmp 	main						;Loop! :D

;-------------------------------------------------------------------------------
; Memory Allocation
;-------------------------------------------------------------------------------
	.data
	.retain
Reserve0: 	.short 	0ACEDh, 0BACEh, 0BEEFh, 0CAFEh, 0DEAFh, 0DEEDh, 0FACEh, 0FADEh, 00808h, 09090h, 0A00Ah, 00BB0h, 0CC00h, 000DDh, 0FFFFh, 01111h
Reserve1: 	.space 	32

;-------------------------------------------------------------------------------
; Stack Pointer definition
;-------------------------------------------------------------------------------
            .global __STACK_END
            .sect   .stack
            
;-------------------------------------------------------------------------------
; Interrupt Vectors
;-------------------------------------------------------------------------------
            .sect   ".reset"                ; MSP430 RESET Vector
            .short  RESET
            
